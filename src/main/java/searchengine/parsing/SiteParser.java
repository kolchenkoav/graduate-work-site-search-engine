package searchengine.parsing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@RequiredArgsConstructor
@Getter
@Setter
@Component
@Scope("prototype")
public class SiteParser {
    private String domain;
    private String url;
    private ParsePage parsedMap;

//    public SiteParser(String url) {
//        this.url = url;
//        this.domain = Utils.getProtocolAndDomain(url);
//    }

    public void getLinks() {
        System.out.println();
        System.out.println("Parsing URL: " + url);

        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        this.parsedMap = new ParsePage();
        this.parsedMap.setUrl(this.url);
        this.parsedMap.setDomain(Utils.getProtocolAndDomain(url));
        this.parsedMap.setParent(null);
        this.parsedMap.setLinks(new ArrayList<>());
        this.parsedMap.setLevel(0);

        pool.execute(parsedMap);
        do {
//            System.out.printf("\rActive threads: %d     Task count: %d    Steal count: %d     Run count: %d",
//                    pool.getActiveThreadCount(), pool.getQueuedTaskCount(), pool.getStealCount(), pool.getRunningThreadCount());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }

            if (pool.isTerminated()) {
                break;
            }
        } while (!parsedMap.isDone());

        pool.shutdown();
        List<String> results = parsedMap.join();
        System.out.println();
        System.out.printf("%s: %d links found.\n", "Total", results.size());
    }

}
