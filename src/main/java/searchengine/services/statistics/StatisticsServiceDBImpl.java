package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SiteList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteE;
import searchengine.model.Status;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
@Primary
public class StatisticsServiceDBImpl implements StatisticsService {

    private final SiteList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaTRepository;

    @Override
    public StatisticsResponse getStatistics() {
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        TotalStatistics total = new TotalStatistics();

        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            SiteE siteE = siteRepository.findByName(site.getName()).orElse(null);
            if (siteE == null) {
                continue;
            }

            int pagesCount = getPageCount(siteE);
            int lemmasCount = getLemmasCount(siteE);

            detailed.add(setDetailedStatisticsItem(site, siteE, pagesCount, lemmasCount));

            total.setPages(total.getPages() + pagesCount);
            total.setLemmas(total.getLemmas() + lemmasCount);
        }
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    private DetailedStatisticsItem setDetailedStatisticsItem(Site site, SiteE siteE, int pagesCount,
        int lemmasCount) {
        DetailedStatisticsItem item = new DetailedStatisticsItem();
        item.setName(site.getName());
        item.setUrl(site.getUrl());
        item.setPages(pagesCount);
        item.setLemmas(lemmasCount);
        item.setStatus(siteE.getStatus().toString());

        item.setError(siteE.getLastError() == null && !siteE.getStatus().equals(Status.FAILED) ? ""
            : siteE.getLastError());

        item.setStatusTime(siteE.getStatusTime().getTime());
        return item;
    }

    private int getLemmasCount(SiteE siteE) {
        int lemmasCount = 0;
        try {
            lemmasCount = lemmaTRepository.countBySiteId(siteE.getSiteId());
        } catch (Exception e) {
            log.warn("lemmasCount = 0");
        }
        return lemmasCount;
    }

    private int getPageCount(SiteE siteE) {
        int pagesCount = 0;
        try {
            pagesCount = pageRepository.countBySiteId(siteE.getSiteId());
        } catch (Exception e) {
            log.warn("pagesCount = 0");
        }
        return pagesCount;
    }
}
