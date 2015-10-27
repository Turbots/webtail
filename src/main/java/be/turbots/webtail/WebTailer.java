package be.turbots.webtail;

import ch.qos.logback.classic.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WebTailer {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(WebTailer.class);

    private static final Logger fileLogger = (Logger) LoggerFactory.getLogger("FILE");

    private static final long SLEEP_TIME_IN_MS = 2000;
    private static final long ONE_MINUTE_IN_MS = 60 * 1000;

    private final HttpClient httpClient = HttpClientBuilder.create().build();

    @Async("myExecutor")
    public void tailLog(final String serverName, final ServerLog serverLog) throws InterruptedException, IOException {
        Thread.currentThread().setName(serverName + "-" + serverLog.getName());
        logger.info("Server [" + serverName + "] Log [" + serverLog.getName() + "] Tailing [" + serverLog.getUrl() + "] to [" + serverLog.getFilename() + "]");
        MDC.put("logFileName", serverLog.getFilename());
        long nrOfBytesRead = 0;
        long sleepTime = SLEEP_TIME_IN_MS;
        long timeSinceNoChange = 0;
        long nrOfBytesReadLastTime;
        while (true) {
            Thread.sleep(sleepTime);
            try {
                nrOfBytesReadLastTime = nrOfBytesRead;
                nrOfBytesRead = tailFile(serverLog.getUrl(), nrOfBytesReadLastTime);
                if (nrOfBytesReadLastTime == nrOfBytesRead) {
                    timeSinceNoChange += sleepTime;
                    if (timeSinceNoChange % ONE_MINUTE_IN_MS == 0) {
                        logger.warn("Log file unchanged for [" + (timeSinceNoChange / ONE_MINUTE_IN_MS) + "] minute(s)");
                    }
                } else {
                    timeSinceNoChange = 0;
                }
                sleepTime = SLEEP_TIME_IN_MS;
            } catch (Exception e) {
                sleepTime *= 2;
                logger.error("Error [" + e + "] occurred - trying again in " + (sleepTime / 1000) + "s");
            }
        }
    }

    private long tailFile(final String url, final long lastRead) throws IOException {
        long currentSize = retrieveLogSize(url);
        if (currentSize == lastRead) {
            return lastRead;
        }
        HttpGet get = new HttpGet(url);
        if (currentSize < lastRead) {
            if (currentSize <= 0) {
                logger.info("Log has been rotated and is empty = [" + currentSize + "] bytes");
                return currentSize;
            } else {
                logger.info("Fetching Range [" + 0 + "-" + currentSize + "] = [" + (lastRead - currentSize) + "] bytes");
                get.addHeader("Range", "bytes=" + 0 + "-" + currentSize);
            }
        } else {
            logger.info("Fetching Range [" + lastRead + "-" + currentSize + "] = [" + (currentSize - lastRead) + "] bytes");
            get.addHeader("Range", "bytes=" + lastRead + "-" + currentSize);
        }
        HttpResponse response = httpClient.execute(get);
        if (response.getEntity().getContentLength() == 0) {
            return 0;
        }
        if (HttpStatus.SC_PARTIAL_CONTENT != response.getStatusLine().getStatusCode()) {
            throw new IllegalStateException("Unexpected status " + response.getStatusLine() + " in \n" + response);
        }
        fileLogger.error(IOUtils.toString(response.getEntity().getContent()));
        return currentSize;
    }

    private long retrieveLogSize(final String url) throws IOException {
        HttpHead head = new HttpHead(url);
        HttpResponse response = httpClient.execute(head);
        return getContentLength(response);
    }

    private long getContentLength(final HttpResponse response) {
        Header contentLength = response.getFirstHeader("Content-Length");
        return Long.valueOf(contentLength.getValue());
    }
}
