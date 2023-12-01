package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.SiteE;
import searchengine.model.Status;
import searchengine.parsing.ParseLemma;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.sql.Timestamp;
import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;
    private final ParseLemma parseLemma;


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        statistics();
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam String url) {
        log.info("*** url: " + url);
        return ResponseEntity.ok(indexingService.indexPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String query,
                                         @RequestParam(required = false) String site,
                                         @RequestParam int offset,
                                         @RequestParam(required = false) int limit) {
        return ResponseEntity.ok(searchService.search(query, site, offset, limit));
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        //parseLemma.parsing("java armchair Ways way. Лошадей и лошадью tables table chair chairs armchairs. Повторное появление леопарда в Осетии позволяет предположить. java Хлеба лошади Java");
        return ResponseEntity.ok(123);
    }
}
