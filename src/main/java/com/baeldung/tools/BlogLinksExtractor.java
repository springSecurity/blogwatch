package com.baeldung.tools;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baeldung.config.GlobalConstants;
import com.baeldung.config.application.SeleniumHeadlessBrowserConfig;

public class BlogLinksExtractor {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    SeleniumHeadlessBrowserConfig headLessBrowser;
    WebDriver webDriver;

    public static void main(String... args) throws Exception {

        BlogLinksExtractor siteMapReader = new BlogLinksExtractor();
        siteMapReader.initize();
        siteMapReader.createArticlesList();
        siteMapReader.createPagesList();
        siteMapReader.cleanup();

    }

    private void cleanup() {
        webDriver.quit();
    }

    private void initize() {
        SeleniumHeadlessBrowserConfig headLessBrowser = new SeleniumHeadlessBrowserConfig();
        headLessBrowser.setTargetEnv(GlobalConstants.TARGET_ENV_WINDOWS);
        headLessBrowser.openNewWindow();
        webDriver = headLessBrowser.getDriver();
    }

    private void createPagesList() throws JDOMException, IOException {
        // webDriver.get(GlobalConstants.PAGES_SITEMAP_URL);
        // Document document = saxBuilder.build(new ByteArrayInputStream(webDriver.getPageSource().getBytes()));
        HttpURLConnection conn;
        URL pageURL = new URL(GlobalConstants.PAGES_SITEMAP_URL);
        conn = (HttpURLConnection) pageURL.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla 5.0");

        SAXBuilder saxBuilder = new SAXBuilder();
        Document document = saxBuilder.build(conn.getInputStream());
        Namespace defaultNamespace = document.getRootElement().getNamespace();
        List<Element> urlElements = document.getRootElement().getChildren("url", defaultNamespace);

        File file = new File(getAbsolutePathToFileInSrc(GlobalConstants.ALL_PAGES_FILE_NAME));
        Path allpagesFilePath = Paths.get(file.getAbsolutePath());
        // Files.write(allpagesFilePath, "".getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        urlElements.forEach(urlNode -> {
            try {
                String url = urlNode.getChild("loc", defaultNamespace).getText().substring(GlobalConstants.BAELDUNG_HOME_PAGE_URL.length());
                if (!urlAlreadyAvailable(allpagesFilePath, url)) {
                    logger.info("New Page found->" + url);
                    Files.write(allpagesFilePath, (url + "\n").getBytes(), StandardOpenOption.APPEND);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private boolean urlAlreadyAvailable(Path allpagesFilePath, String url) throws IOException {
        try (Stream<String> lines = Files.lines(allpagesFilePath)) {
            Optional<String> hasPassword = lines.filter(s -> (s + "/").contains(url + "/")).findFirst();
            if (hasPassword.isPresent()) {
                return true;
            }
        }
        return false;
    }

    private void createArticlesList() {
        webDriver.get(GlobalConstants.FULL_ARCHIVE_URL);
        List<WebElement> archiveURLElemets = webDriver.findElements(By.xpath("//ul[contains(@class, 'car-list')]//a"));
        File file = new File(getAbsolutePathToFileInSrc(GlobalConstants.ALL_ARTICLES_FILE_NAME));
        Path allArtilcesFilePath = Paths.get(file.getAbsolutePath());
        // Files.write(allArtilcesFilePath, "".getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        archiveURLElemets.forEach(anchorTag -> {
            try {
                String url = anchorTag.getAttribute("href").substring(GlobalConstants.BAELDUNG_HOME_PAGE_URL.length());
                if (!urlAlreadyAvailable(allArtilcesFilePath, url) && !isFlaggedArticle(url)) {
                    logger.info("New Article found->" + url);
                    Files.write(allArtilcesFilePath, (anchorTag.getAttribute("href").substring(GlobalConstants.BAELDUNG_HOME_PAGE_URL.length()) + "\n").getBytes(), StandardOpenOption.APPEND);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    private String getAbsolutePathToFileInSrc(String fileName) {
        return new File(BlogLinksExtractor.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getParent() + "/src/main/resources/blog-url-list/" + fileName;
    }

    private boolean isFlaggedArticle(String url) {
        return GlobalConstants.flaggedArticles.stream().anyMatch(str -> str.equals(url + "/"));
    }

}
