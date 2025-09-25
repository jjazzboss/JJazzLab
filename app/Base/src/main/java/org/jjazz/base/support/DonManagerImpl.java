package org.jjazz.base.support;

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.io.BaseEncoding;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.base.spi.AboutDialogInfoProvider;
import org.jjazz.utilities.api.ResUtil;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.OnShowing;

/**
 * Auhtorization manager implementation.
 * <p>
 * Launch the nagging dialog upon startup if required.
 */
@OnShowing
@ServiceProvider(service = DonManager.class, position = Integer.MIN_VALUE)
public class DonManagerImpl implements Runnable, DonManager
{

    private static final String PREF_NB_RUNS = "ImageSize";
    private static final String PREF_EXPIRATION_DAY_FROM_2020 = "BufferWidth";  // Number of days from 2020/01/01
    private static final String PREF_CURRENT_CODE_DATE = "BufferHash";  // Date of current code
    private static final int NO_NOTIF_THRESHOLD = 34322;    // If nb runs greater than this, it means a valid donation code was entered
    private static final int FIRST_NOTIF_NB_RUN_THRESHOLD = 8;
    private static final int FIRST_WAIT_TIME = 10;
    private static final int SECOND_NOTIF_NB_RUN_THRESHOLD = 15;
    private static final int SECOND_WAIT_TIME = 20;
    private static final Preferences prefs = NbPreferences.forModule(DonManagerImpl.class);
    private static final Logger LOGGER = Logger.getLogger(DonManagerImpl.class.getSimpleName());


    /**
     * Executed upon JJazzLab startup.
     * <p>
     * Show the donate dialog if no registered code or registered code has expired.
     */
    @Override
    public void run()
    {
        int nbRuns = getNbRuns();
        if (nbRuns >= NO_NOTIF_THRESHOLD)
        {
            // We had a valid code, check expiration date
            int nbDays = getRegisteredCodeExpirationDateAsNbDays2020();
            if (nbDays >= 0 && !isExpired(nbDays))
            {
                // Everything OK
                return;
            } else
            {
                // Expired, reset everything
                removeRegisteredCodePreferences();
                nbRuns = FIRST_NOTIF_NB_RUN_THRESHOLD;
                Analytics.logEvent("Code expired");
            }
        }

        nbRuns++;
        prefs.put(PREF_NB_RUNS, encodeInt(nbRuns));
        Analytics.setProperties(Analytics.buildMap("Code validity", false));


        if (nbRuns < FIRST_NOTIF_NB_RUN_THRESHOLD)
        {
            return;
        }


        // Show dialog
        int waitSeconds = FIRST_WAIT_TIME;
        boolean modal = false;
        if (nbRuns >= SECOND_NOTIF_NB_RUN_THRESHOLD)
        {
            waitSeconds = SECOND_WAIT_TIME;
            modal = true;
        }
        DonNotifDialog dlg = new DonNotifDialog(waitSeconds, modal);
        dlg.setVisible(true);
    }

    /**
     * Register a code.
     * <p>
     * Check if parameters are valid and register the code if OK.
     *
     * @param code
     * @param email
     * @throws java.text.ParseException If code and/or email are not valid.
     */
    @Override
    public void registerCode(String code, String email) throws ParseException
    {
        if (code == null || code.isBlank() || email == null || email.isBlank())
        {
            String msg = ResUtil.getString(getClass(), "EmptyCodeOrEmail");
            throw new ParseException(msg, 0);
        }

        String code1 = decode2(code);
        String code2;
        try
        {
            byte[] bytes = BaseEncoding.base64().decode(code1);
            code2 = new String(bytes);
        } catch (IllegalArgumentException ex)
        {
            String msg = ResUtil.getString(getClass(), "InvalidCode");
            LOGGER.log(Level.WARNING, "** Donation code registration error: {0}", msg);
            throw new ParseException(msg, 0);
        }


        String strs[] = code2.split(":");
        if (strs.length != 4)
        {
            String msg = ResUtil.getString(getClass(), "InvalidCode");
            LOGGER.log(Level.WARNING, "** Donation code registration error: {0}", msg);
            throw new ParseException(msg, 0);
        }


        // Email
        String code_email = strs[0];
        if (!code_email.equals(email))
        {
            String msg = ResUtil.getString(getClass(), "MailCodeMismatch");
            LOGGER.log(Level.WARNING, "** Donation code registration error: {0}", msg);
            throw new ParseException(msg, 0);
        }


        // Donation date
        String code_date = strs[1];        // ex 2021-08-27
        int code_dateNbDays2020 = getNbDaysFrom2020FromDateString(code_date);
        if (code_dateNbDays2020 < 0)
        {
            String msg = ResUtil.getString(getClass(), "InvalidDateFormat", code_date);
            LOGGER.log(Level.WARNING, "** Donation code registration error: {0}", msg);
            throw new ParseException(msg, 0);
        }


        // Donation amount
        String code_amountEuro = strs[2];        // ex 10
        float amountEuro = 1;
        try
        {
            amountEuro = Float.valueOf(code_amountEuro);
        } catch (NumberFormatException ex)
        {
            LOGGER.log(Level.WARNING, "** Donation code registration warning: invalid amount={0} using default", code_amountEuro);
        }


        // Expiration date
        String strNbDaysFrom2020 = strs[3];        // ex 1020              
        int code_expirationDateNbDaysFrom2020;
        try
        {
            code_expirationDateNbDaysFrom2020 = Integer.valueOf(strNbDaysFrom2020);
        } catch (NumberFormatException ex)
        {
            String msg = "InvalidNbd: "+ strNbDaysFrom2020;
            LOGGER.log(Level.WARNING, "** Donation code registration error: {0}", msg);
            throw new ParseException(msg, 0);
        }
        if (isExpired(code_expirationDateNbDaysFrom2020))
        {
            String msg = ResUtil.getString(getClass(), "ConfigExpired");
            LOGGER.log(Level.WARNING, "** Donation code registration error: {0}", msg);
            throw new ParseException(msg, 0);
        }


        // Check if there is a current valid donation code
        if (hasValidRegisteredCode())
        {
            int registeredCode_dateNbDays2020 = getRegisteredCodeDateAsNbDays2020();
            int registeredCode_expirationNbDays2020 = getRegisteredCodeExpirationDateAsNbDays2020();

            // Extend the registered code wih the new duration, except if too close from the registered date with small amount
            // (avoid pay 1€, then 1€ the day after, you would get equivalent of 20€!)           
            if (registeredCode_expirationNbDays2020 > 0
                    && ((code_dateNbDays2020 - registeredCode_dateNbDays2020 >= 90) || amountEuro >= 10))
            {
                int extraDays = registeredCode_expirationNbDays2020 - code_dateNbDays2020;
                code_expirationDateNbDaysFrom2020 += extraDays;
            }

            // In any case make sure we don't make expiration date earlier than before
            code_expirationDateNbDaysFrom2020 = Math.max(code_expirationDateNbDaysFrom2020, registeredCode_expirationNbDays2020);

        }


        prefs.put(PREF_NB_RUNS, encodeInt(NO_NOTIF_THRESHOLD + ((int) (Math.random() * 100))));
        prefs.put(PREF_EXPIRATION_DAY_FROM_2020, encodeInt(code_expirationDateNbDaysFrom2020));
        prefs.put(PREF_CURRENT_CODE_DATE, encodeInt(code_dateNbDays2020));
        LOGGER.log(Level.INFO, "** New donation code successfully registered c1={0} c2={1}", new Object[]
        {
            code_dateNbDays2020,
            code_expirationDateNbDaysFrom2020
        });
        Analytics.logEvent("Code registered");
        Analytics.setProperties(Analytics.buildMap("Code validity", true, "Code amount", code_amountEuro));
    }

    /**
     * Check if there is a registered code and it's still valid (not expired).
     *
     * @return
     */
    @Override
    public boolean hasValidRegisteredCode()
    {
        boolean b = false;
        if (getNbRuns() >= NO_NOTIF_THRESHOLD)
        {
            int nbDays2020 = getRegisteredCodeExpirationDateAsNbDays2020();
            if (nbDays2020 >= 0)
            {
                b = !isExpired(nbDays2020);
            }
        }

        return b;
    }


    /**
     * Return expiration date of last registered code (might be expired).
     *
     * @return Null if no valid current donation code.
     */
    @Override
    public Instant getRegisteredCodeExpirationDate()
    {
        Instant res = null;
        int nbDaysFrom2020 = getRegisteredCodeExpirationDateAsNbDays2020();
        if (nbDaysFrom2020 >= 0)
        {
            var instant2020 = Instant.parse("2020-01-01T00:00:00.00Z");
            res = instant2020.plus(Duration.ofDays(nbDaysFrom2020));
        }
        return res;
    }

    /**
     * Return creation date in nb of days since 2020 of the last registered code (might be expired).
     *
     * @return -1 if no valid code.
     */
    @Override
    public int getRegisteredCodeDateAsNbDays2020()
    {
        String s = prefs.get(PREF_CURRENT_CODE_DATE, null);
        if (s == null)
        {
            return -1;
        }
        int nbDaysFrom2020 = -1;

        try
        {
            nbDaysFrom2020 = decodeInt(s);
        } catch (NumberFormatException ex)
        {
            // Nothing
        }

        return nbDaysFrom2020;
    }

    /**
     *
     * @return A string like "4 Jul 2023". Null if no expiration date stored.
     */
    @Override
    public String getRegisteredCodeExpirationDateAsString()
    {
        String res = null;
        var instant = getRegisteredCodeExpirationDate();
        if (instant != null)
        {
            SimpleDateFormat formatter = new SimpleDateFormat("d MMM yyyy");
            res = formatter.format(Date.from(instant));
        }
        return res;
    }

    /**
     * The expiration date of the last registered code, if available.
     *
     * @return The number of days from the start of 2020. Or -1 if no expiration date available.
     */
    @Override
    public int getRegisteredCodeExpirationDateAsNbDays2020()
    {
        int res = -1;
        String strExpirationDay = prefs.get(PREF_EXPIRATION_DAY_FROM_2020, null);
        if (strExpirationDay != null)
        {
            try
            {
                res = decodeInt(strExpirationDay);
            } catch (NumberFormatException ex)
            {
                LOGGER.log(Level.INFO, "Invalid BufferWidth value={0}, using default value", strExpirationDay);
                prefs.remove(PREF_EXPIRATION_DAY_FROM_2020);
            }
        }
        return res;
    }


    // ===============================================================================
    // Private methods
    // ===============================================================================
    private int getNbRuns()
    {
        int res = 0;
        String strNbRuns = prefs.get(PREF_NB_RUNS, null);
        if (strNbRuns != null)
        {
            try
            {
                res = decodeInt(strNbRuns);
            } catch (NumberFormatException ex)
            {
                LOGGER.log(Level.INFO, "Invalid ImageSize value={0}, using default value", strNbRuns);
                prefs.remove(PREF_NB_RUNS);
            }
        }
        return res;
    }


    private char encodeChar(char c, int n)
    {
        char res = c;
        if (c >= 'C' && c <= 'V')
        {
            int start = 'C';
            int end = 'V';
            int size = end - start + 1;
            int relpos = c - start;
            int pos = start + (relpos + n) % size;
            res = (char) pos;
            // echo "c=$c n=$n start=$start end=$end size=$size relpos=$relpos pos=$pos res=$res\n";
        } else if (c >= 'e' && c <= 'w')
        {
            int start = 'e';
            int end = 'w';
            int size = end - start + 1;
            int relpos = c - start;
            int pos = start + (relpos + n) % size;
            res = (char) pos;
        }
        return res;
    }

    private char decodeChar(char c, int n)
    {
        int start, end;
        int size = 0;
        if (c >= 'C' && c <= 'V')
        {
            start = 'C';
            end = 'V';
            size = end - start + 1;
        } else if (c >= 'e' && c <= 'w')
        {
            start = 'e';
            end = 'w';
            size = end - start + 1;
        }

        return encodeChar(c, n * (size - 1));
    }

    private String compute2(String s)
    {
        StringBuilder res = new StringBuilder();
        int pos = 1;
        for (char c : s.toCharArray())
        {
            char ec = encodeChar(c, pos * 2);
            res.append(ec);
            pos++;
        }

        return res.toString();
    }

    private String decode2(String s)
    {
        StringBuilder res = new StringBuilder();
        int pos = 1;
        for (char c : s.toCharArray())
        {
            char dc = decodeChar(c, pos * 2);
            res.append(dc);
            pos++;
        }
        return res.toString();
    }


    /**
     * Really not strong encoding, just better than nothing.
     *
     * @param value Must be a 16-bit positive value [0-0xFFFF].
     * @return
     */
    private String encodeInt(int value)
    {
        checkArgument(value <= 0xFFFF);
        int b0 = value & 1;
        int oddBits = value & 0b1010101010101010;
        int evenBits = value & 0b0101010101010101;
        evenBits ^= 0b0101010101010101;
        evenBits = evenBits >> 2;
        evenBits |= (b0 << 14);
        int res = evenBits | oddBits;
        return Integer.toHexString(res);
    }

    /**
     * Decode a string encoded with encodeInt().
     *
     * @param s
     * @return A value in the range [0-0xFFFF].
     * @throws NumberFormatException
     * @see #encodeInt(int)
     */
    private int decodeInt(String s) throws NumberFormatException
    {
        int hex = Integer.parseInt(s, 16);
        if (hex > 0xFFFF)
        {
            throw new NumberFormatException("hex value is > 0xFFFF: " + Integer.toHexString(hex));
        }
        int b0 = (hex >> 14) & 1;
        int oddBits = hex & 0b1010101010101010;
        int evenBits = hex & 0b0101010101010101;
        evenBits ^= 0b0101010101010101;
        evenBits = (evenBits << 2) & 0xFFFF;
        evenBits |= b0;
        int res = evenBits | oddBits;
        return res;
    }

    private boolean isExpired(int nbDaysFrom2020)
    {
        var instantExpire = getInstant(nbDaysFrom2020);
        return Instant.now().isAfter(instantExpire);
    }

    private Instant getInstant(int nbDaysFrom2020)
    {
        var instant2020 = Instant.parse("2020-01-01T00:00:00.00Z");
        return instant2020.plus(Duration.ofDays(nbDaysFrom2020));
    }

    /**
     * Convert the date string in nb days from 2020.
     *
     * @param YYYY_MM_DD A string like "2020-20-10"
     * @return -1 if string is invalid
     */
    private int getNbDaysFrom2020FromDateString(String YYYY_MM_DD)
    {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        try
        {
            LocalDate date1 = LocalDate.parse("2020-01-01", dtf);
            LocalDate date2 = LocalDate.parse(YYYY_MM_DD, dtf);
            return (int) ChronoUnit.DAYS.between(date1, date2);
        } catch (DateTimeParseException e)
        {
            return -1;
        }

    }

    /**
     * Clean all preferences for the registered code.
     */
    private void removeRegisteredCodePreferences()
    {
        prefs.remove(PREF_EXPIRATION_DAY_FROM_2020);
        prefs.remove(PREF_CURRENT_CODE_DATE);
    }


    // ===============================================================================
    // Inner classes
    // ===============================================================================
    // =====================================================================================
    // AboutDialogInfoProvider implementation
    // =====================================================================================
    @ServiceProvider(service = AboutDialogInfoProvider.class)
    static public class AboutDialogInfoProviderImpl implements AboutDialogInfoProvider
    {

        @Override
        public String getInfo()
        {
            String res = "";
            var impl = DonManager.getDefault();
            if (impl != null && impl.hasValidRegisteredCode())
            {
                res = ResUtil.getString(getClass(), "CurrentCodeExpirationDate", impl.getRegisteredCodeExpirationDateAsString());
            }
            return res;
        }

    }

}
