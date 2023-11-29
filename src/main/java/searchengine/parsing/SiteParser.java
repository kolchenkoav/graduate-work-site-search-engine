package searchengine.parsing;


import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

//@Component
public class SiteParser {
    private int siteId;
    private String domain;
    private String url;
    private ParsePage parsedMap;

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        System.out.println("### SiteParser.setSiteId() -> siteId: "+siteId);
        this.siteId = siteId;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void getLinks() {
        System.out.println();
        System.out.println("Parsing URL: " + url);
        ForkJoinPool pool = new ForkJoinPool();

        parsedMap = new ParsePage();
        parsedMap.setUrl(url);
        parsedMap.setDomain(domain);
        parsedMap.setParent(null);
        parsedMap.setLinks(new ArrayList<>());
        parsedMap.setLevel(0);
        parsedMap.setSiteId(siteId);

        pool.execute(parsedMap);
        do {
//            System.out.printf("\rActive threads: %d     Task count: %d    Steal count: %d     Run count: %d",
//                    pool.getActiveThreadCount(), pool.getQueuedTaskCount(), pool.getStealCount(), pool.getRunningThreadCount());
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                //logger.error(ExceptionUtils.getStackTrace(e));
            }
        } while (!parsedMap.isDone());

        pool.shutdown();
        List<String> results = parsedMap.join();
        System.out.println();
        System.out.printf("%s: %d links found.\n", "Total", results.size());
    }
}
