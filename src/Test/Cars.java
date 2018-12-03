package Test;

import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.concurrent.TimeUnit.SECONDS;


public class Cars {
    private static final Logger LOG = Logger.getLogger(Cars.class);

    private static final DateFormat REGISTRATION_DATE_FORMAT = new SimpleDateFormat("MM/yyyy");

    // Location of the property file with test settings
    private static final String PROPERTY_FILE = "/car-shop.properties";
    private static final Properties PROPERTIES = new Properties();

    private static final Date START_DATE;
    private static final String START_URL;

    private static final WebDriver driver;
    private static       int prevItemPrice = Integer.MAX_VALUE;

    // Initialize the constant (final) variables
    static {
        InputStream propertyStream = Cars.class.getResourceAsStream(PROPERTY_FILE);
        try {
            PROPERTIES.load(propertyStream);
            propertyStream.close();
        }
        catch (Exception e) {
            LOG.fatal("Can't load application property. Is the file '" + PROPERTY_FILE + "' in classpath?", e);
            System.exit(1);
        }

        // Define the minimal Date for the car registration
        // (that is the filter for `FROM​ 2015' - the year is defined in the property file)
        int minYear = Integer.parseInt( PROPERTIES.getProperty("yearMin") );
        START_DATE = new GregorianCalendar(minYear, GregorianCalendar.JANUARY, 1).getTime();

        // Define the start URL - including parameters:
        // - a filter for the first registration date (Erstzulassung)
        // - a sorting type - by price in descending order ( Höchster Preis )
        START_URL = PROPERTIES.getProperty("car-shop.base.url") + "?sort=PRICE_DESC&yearMin=" + minYear;

        // Define the Chrome driver settings:
        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(10, SECONDS);
        driver.manage().window().maximize();
    }

    // Locate date in the given text and parses it as MM/YYYY
    private static Date parseDate(String text) throws ParseException {
        String strDate = text.replaceAll("[^/0-9]", ""); // drop anything except for slashes and digits
        return REGISTRATION_DATE_FORMAT.parse(strDate);
    }

    // Locate price in the given text and parses it (using the Germany's locale)
    private static int parsePrice(String text) throws ParseException {
        String strPrice = text.replaceAll("[^.0-9]", ""); // drop anything except for dots and digits
        return NumberFormat.getNumberInstance(Locale.GERMANY).parse(strPrice).intValue();
    }


    // Run a check for a single page and return false if the check is failed
    private static boolean checkPage(int currentPage, final WebElement resultsDiv) throws ParseException {
        List<WebElement> items = resultsDiv.findElements(By.xpath("div[@data-qa-selector='ad-items']//a[@data-qa-selector='ad']"));
        LOG.debug("Found " + items.size() + " results on the page #" + currentPage);

        int count = 0;
        for (WebElement item : items) {
            // Locate the Registration date element and parse it:
            WebElement regDateLi = item.findElement(By.xpath("descendant::ul[@data-qa-selector='spec-list']/li[@data-qa-selector='spec'][1]"));
            Date registrationDate = parseDate( regDateLi.getText() );

            // Locate the Price element and parse it:
            String priceText = item.findElement(By.xpath("descendant::div[@data-qa-selector='price']")).getText();
            int currentPrice = parsePrice(priceText);

            // Log info about the item that we're processing now:
            LOG.debug("Item #" + currentPage + "-" + (++count) + ": date=" +
                    REGISTRATION_DATE_FORMAT.format(registrationDate) + ", price=" + currentPrice
            );

            // Check if the registration date is correct:
            if ( registrationDate.before(START_DATE) ) {
                LOG.error("There is an item with a registration date earlier than " + REGISTRATION_DATE_FORMAT.format(START_DATE));
                return false;
            }

            // Check if the items are ordered correctly:
            if (currentPrice > prevItemPrice) {
                LOG.error("The item has a bigger price than previous one!");
                return false;
            }
            prevItemPrice = currentPrice;
        }

        return true;
    }

    // The entry point
    public static void main(String[] args) throws Exception {
        driver.get( START_URL );

        // Locate the last page number:
        WebElement firstPageResults = getFoundResultsDiv();
        int lastPage = Integer.parseInt( firstPageResults.findElement(By.xpath("descendant::ul[@class='pagination']/li[last()-2]")).getText() );

        // Retrieve a total number of cars on all pages:
        String numOfCarsFound = driver.findElement(By.xpath("//div[@data-qa-selector='results-amount']")).getText();
        int totalCarsFound = Integer.parseInt( numOfCarsFound.replaceAll("[^0-9]", "") );
        LOG.info("Found " + totalCarsFound + " cars on " + lastPage + " pages");

        // Iterate over the pages and and verify them one by one,
        // exiting the cycle if the verification is failed at some point:
        LOG.info("Processing page #1:");
        if ( checkPage(1, firstPageResults) ) {
            for (int page=2; page <= lastPage; ++page) {
                driver.get( START_URL + "&page=" + page );
                WebElement pageResults = getFoundResultsDiv();
                LOG.info("Processing page #" + page + ":");
                if (! checkPage(page, pageResults)) {
                    // found an error, there is no sense to continue
                    return;
                }
            }
            LOG.info("OK, validation is complete. No issues are found");
        }
    }

    // Locate the main <div> with the found search results:
    private static WebElement getFoundResultsDiv() {
        return driver.findElement(By.xpath("//div[@data-qa-selector='results-found']"));
    }
}

