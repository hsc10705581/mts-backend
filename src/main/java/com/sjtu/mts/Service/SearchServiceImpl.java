package com.sjtu.mts.Service;

import com.sjtu.mts.Entity.*;
import com.sjtu.mts.Repository.AreaRepository;
import com.sjtu.mts.Repository.FangAnRepository;
import com.sjtu.mts.Repository.SensitiveWordRepository;
import com.sjtu.mts.Response.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final AreaRepository areaRepository;
    private final FangAnRepository fangAnRepository;
    private final SensitiveWordRepository sensitiveWordRepository;


    public SearchServiceImpl(ElasticsearchOperations elasticsearchOperations,AreaRepository areaRepository,FangAnRepository fangAnRepository,SensitiveWordRepository sensitiveWordRepository)
    {
        this.elasticsearchOperations = elasticsearchOperations;
        this.areaRepository = areaRepository;
        this.fangAnRepository = fangAnRepository;
        this.sensitiveWordRepository = sensitiveWordRepository;
    }

    @Override
    public DataResponse Search(String keyword, String cflag, String startPublishedDay, String endPublishedDay,
                               String fromType, int page, int pageSize, int timeOrder)
    {
        Criteria criteria = new Criteria();
        if (!keyword.isEmpty())
        {
            String[] searchSplitArray = keyword.trim().split("\\s+");;
            for (String searchString : searchSplitArray) {
                criteria.subCriteria(new Criteria().and("content").contains(searchString).
                        or("title").contains(searchString));
            }
        }
        if (!cflag.isEmpty())
        {
            criteria.subCriteria(new Criteria().and("cflag").is(cflag));
        }
        if (!startPublishedDay.isEmpty() && !endPublishedDay.isEmpty())
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                Date startDate = sdf.parse(startPublishedDay);
                Date endDate = sdf.parse(endPublishedDay);
                criteria.subCriteria(new Criteria().and("publishedDay").between(startDate, endDate));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (!fromType.isEmpty())
        {
            criteria.subCriteria(new Criteria().and("fromType").is(fromType));
        }
        CriteriaQuery query = new CriteriaQuery(criteria);
        if (timeOrder == 0) {
            query.setPageable(PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "publishedDay")));
        }
        else {
            query.setPageable(PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "publishedDay")));
        }
        SearchHits<Data> searchHits = this.elasticsearchOperations.search(query, Data.class);
        SearchPage<Data> searchPage = SearchHitSupport.searchPageFor(searchHits, query.getPageable());
        long hitNumber = this.elasticsearchOperations.count(query, Data.class);

        List<Data> pageDataContent = new ArrayList<>();
        for (SearchHit<Data> hit : searchPage.getSearchHits())
        {
            pageDataContent.add(hit.getContent());
        }

        DataResponse result = new DataResponse();
        result.setHitNumber(hitNumber);
        result.setDataContent(pageDataContent);

        return result;
    }
    @Override
    public ResourceCountResponse globalSearchResourceCount(String keyword, String startPublishedDay, String endPublishedDay) {
        List<Long> resultList = new ArrayList<>();
        for (int fromType = 1; fromType <= 7 ; fromType++) {
            Criteria criteria = new Criteria();
            if (!keyword.isEmpty())
            {
                String[] searchSplitArray = keyword.trim().split("\\s+");;
                for (String searchString : searchSplitArray) {
                    criteria.subCriteria(new Criteria().and("content").contains(searchString).
                            or("title").contains(searchString));
                }
            }
            if (!startPublishedDay.isEmpty() && !endPublishedDay.isEmpty())
            {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                try {
                    Date startDate = sdf.parse(startPublishedDay);
                    Date endDate = sdf.parse(endPublishedDay);
                    criteria.subCriteria(new Criteria().and("publishedDay").between(startDate, endDate));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            criteria.subCriteria(new Criteria().and("fromType").is(fromType));
            CriteriaQuery query = new CriteriaQuery(criteria);
            long searchHitCount = this.elasticsearchOperations.count(query, Data.class);
            resultList.add(searchHitCount);
        }
        return new ResourceCountResponse(resultList.get(0), resultList.get(1), resultList.get(2),
                resultList.get(3), resultList.get(4), resultList.get(5), resultList.get(6));
    }
    @Override
    public CflagCountResponse globalSearchCflagCount(String keyword, String startPublishedDay, String endPublishedDay) {
        List<Long> resultList = new ArrayList<>();
        for (int cflag = 1; cflag <= 2 ; cflag++) {
            Criteria criteria = new Criteria();
            if (!keyword.isEmpty())
            {
                String[] searchSplitArray = keyword.trim().split("\\s+");;
                for (String searchString : searchSplitArray) {
                    criteria.subCriteria(new Criteria().and("content").contains(searchString).
                            or("title").contains(searchString));
                }
            }
            if (!startPublishedDay.isEmpty() && !endPublishedDay.isEmpty())
            {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                try {
                    Date startDate = sdf.parse(startPublishedDay);
                    Date endDate = sdf.parse(endPublishedDay);
                    criteria.subCriteria(new Criteria().and("publishedDay").between(startDate, endDate));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            criteria.subCriteria(new Criteria().and("cflag").is(cflag));
            CriteriaQuery query = new CriteriaQuery(criteria);
            long searchHitCount = this.elasticsearchOperations.count(query, Data.class);
            resultList.add(searchHitCount);
        }
        return new CflagCountResponse(resultList.get(0), resultList.get(1));
    }
    @Override
    public AmountTrendResponse globalSearchTrendCount(String keyword, String startPublishedDay, String endPublishedDay) {
        int pointNum = 6;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<Date> dateList = new ArrayList<>();
        try {
            Date startDate = sdf.parse(startPublishedDay);
            Date endDate = sdf.parse(endPublishedDay);
            dateList.add(startDate);
            for (int i = 1; i <= pointNum; i++){
                Date dt = new Date((long)(startDate.getTime()+(endDate.getTime()-startDate.getTime())*i/(double)pointNum));
                dateList.add(dt);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        List<String> timeRange = new ArrayList<>();
        List<List<Long>> fromTypeResultList = new ArrayList<>();
        for (int fromType = 0; fromType <= 7 ; fromType++) {
            List<Long> resultList = new ArrayList<>();
            for (int j = 0; j < pointNum; j++) {
                Criteria criteria = new Criteria();
                if (!keyword.isEmpty())
                {
                    String[] searchSplitArray = keyword.trim().split("\\s+");;
                    for (String searchString : searchSplitArray) {
                        criteria.subCriteria(new Criteria().and("content").contains(searchString).
                                or("title").contains(searchString));
                    }
                }
                // fromType 0 indicates searching all fromTypes
                if (fromType != 0) {
                    criteria.subCriteria(new Criteria().and("fromType").is(fromType));
                }
                else {
                    // only add once
                    timeRange.add(sdf.format(dateList.get(j)) + " to " + sdf.format(dateList.get(j + 1)));
                }
                criteria.subCriteria(new Criteria().and("publishedDay").between(dateList.get(j), dateList.get(j + 1)));
                CriteriaQuery query = new CriteriaQuery(criteria);
                long searchHitCount = this.elasticsearchOperations.count(query, Data.class);
                resultList.add(searchHitCount);
            }
            fromTypeResultList.add(resultList);
        }
        return new AmountTrendResponse(timeRange, fromTypeResultList.get(0),
                fromTypeResultList.get(1), fromTypeResultList.get(2), fromTypeResultList.get(3),
                fromTypeResultList.get(4), fromTypeResultList.get(5), fromTypeResultList.get(6),
                fromTypeResultList.get(7));
    }

    @Override
    public AreaAnalysisResponse countArea(String keyword, String startPublishedDay, String endPublishedDay){
        List<Long> resultList = new ArrayList<>();
        List<Integer> codeids = Arrays.asList(11,12,13,14,15,21,22,23,31,32,33,34,35,36,37,41,42,43,44,45,46,50,51,52,53,54,61,62,63,64,65,71,81,91);
        for(int i =0;i<codeids.size();i++){
            Criteria criteria = new Criteria();
            if (!keyword.isEmpty())
            {
                String[] searchSplitArray = keyword.trim().split("\\s+");;
                for (String searchString : searchSplitArray) {
                    criteria.subCriteria(new Criteria().and("content").contains(searchString).
                            or("title").contains(searchString));
                }
            }
            if (!startPublishedDay.isEmpty() && !endPublishedDay.isEmpty())
            {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                try {
                    Date startDate = sdf.parse(startPublishedDay);
                    Date endDate = sdf.parse(endPublishedDay);
                    criteria.subCriteria(new Criteria().and("publishedDay").between(startDate, endDate));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            List<String> citys;
            citys = areaRepository.findCityNameByCodeid(codeids.get(i));
            for(int j=0;j<citys.size();j++){
                citys.set(j,citys.get(j).replaceAll("\\s*", ""));
                if(citys.get(j).contains("市辖")||citys.get(j).contains("县辖")){
                    citys.remove(j);
                }
            }
            citys = (List) citys.stream().distinct().collect(Collectors.toList());//去重
            //System.out.println(Arrays.toString(citys.toArray()));
            criteria.subCriteria(new Criteria("content").in(citys).or("title").in(citys));
            CriteriaQuery query = new CriteriaQuery(criteria);
            long searchHitCount = this.elasticsearchOperations.count(query, Data.class);
            resultList.add(searchHitCount);

        }
        return new AreaAnalysisResponse(resultList.get(0),resultList.get(1),resultList.get(2),resultList.get(3),resultList.get(4),
                resultList.get(5),resultList.get(6),resultList.get(7),resultList.get(8),resultList.get(9),resultList.get(10),resultList.get(11),
                resultList.get(12),resultList.get(13),resultList.get(14),resultList.get(15),resultList.get(16),resultList.get(17),
                resultList.get(18),resultList.get(19),resultList.get(20),resultList.get(21),resultList.get(22),resultList.get(23),
                resultList.get(24),resultList.get(25),resultList.get(26),resultList.get(27),resultList.get(28),resultList.get(29),
                resultList.get(30),resultList.get(31),resultList.get(32),resultList.get(33));
    }

    @Override
    public DataResponse fangAnSearch(long fid,String cflag, String startPublishedDay, String endPublishedDay,
                                     String fromType, int page, int pageSize, int timeOrder){
        FangAn fangAn = fangAnRepository.findByFid(fid);
        int matchType = fangAn.getMatchType();
        String regionKeyword = fangAn.getRegionKeyword();
        int regionKeywordMatch = fangAn.getRegionKeywordMatch();

        String roleKeyword = fangAn.getRoleKeyword();
        int roleKeywordMatch = fangAn.getRoleKeywordMatch();

        String eventKeyword = fangAn.getEventKeyword();
        int eventKeywordMatch = fangAn.getEventKeywordMatch();
        Criteria criteria = new Criteria();

        if (!roleKeyword.isEmpty())
        {
            String[] searchSplitArray1 = roleKeyword.trim().split("\\s+");
            List<String>searchSplitArray = Arrays.asList(searchSplitArray1);
            System.out.println(searchSplitArray.size());
            System.out.println(searchSplitArray.get(0));
            if(searchSplitArray.size()>1){
                if(roleKeywordMatch==1){
                    for (String searchString : searchSplitArray) {

                        criteria.subCriteria(new Criteria().and("content").contains(searchString).
                                or("title").contains(searchString));
                    }
                }else {
                    criteria.subCriteria(new Criteria("content").in(searchSplitArray).or("title").in(searchSplitArray));
                }
            }else {
                criteria.subCriteria(new Criteria().and("content").contains(searchSplitArray.get(0)).
                        or("title").contains(searchSplitArray.get(0)));
            }


        }
        if (!eventKeyword.isEmpty())
        {
            String[] searchSplitArray1 = eventKeyword.trim().split("\\s+");
            List<String>searchSplitArray = Arrays.asList(searchSplitArray1);
            System.out.println(searchSplitArray.size());
            if(searchSplitArray.size()>1){
                if(eventKeywordMatch==1){
                    for (String searchString : searchSplitArray) {

                        criteria.subCriteria(new Criteria().and("content").contains(searchString).
                                or("title").contains(searchString));
                    }
                }else {
                    criteria.subCriteria(new Criteria("content").in(searchSplitArray).or("title").in(searchSplitArray));
                }
            }else {
                criteria.subCriteria(new Criteria().and("content").contains(searchSplitArray.get(0)).
                        or("title").contains(searchSplitArray.get(0)));
            }


        }
        if (!regionKeyword.isEmpty())
        {

            String[] searchSplitArray1 = regionKeyword.trim().split("\\s+");
            List<String>searchSplitArray = Arrays.asList(searchSplitArray1);
            if(searchSplitArray.size()==1 ){
                List<Integer> codeid  = areaRepository.findCodeidByCityName(searchSplitArray.get(0));
                List<String> citys = new ArrayList<>();
                for (Integer co:codeid){
                    List<String> tmp = areaRepository.findCityNameByCodeid(co) ;
                    for(int i=0;i<tmp.size();i++){
                        tmp.set(i,tmp.get(i).replaceAll("\\s*", ""));
                        if(tmp.get(i).contains("市辖")||tmp.get(i).contains("县辖")){
                            tmp.remove(i);
                        }
                    }
                    citys.addAll(tmp);
                }

                citys = (List) citys.stream().distinct().collect(Collectors.toList());//去重
                //System.out.println(Arrays.toString(citys.toArray()));
                criteria.subCriteria(new Criteria("content").in(citys).or("title").in(citys));
            }else if(searchSplitArray.size()>1 && regionKeywordMatch == 1){
                for (String searchString : searchSplitArray){
                    List<Integer> codeid  = areaRepository.findCodeidByCityName(searchString);
                    List<String> citys = new ArrayList<>();
                    for (Integer co:codeid){
                        List<String> tmp = areaRepository.findCityNameByCodeid(co) ;
                        for(int i=0;i<tmp.size();i++){
                            tmp.set(i,tmp.get(i).replaceAll("\\s*", ""));
                            if(tmp.get(i).contains("市辖")||tmp.get(i).contains("县辖")){
                                tmp.remove(i);
                            }
                        }
                        citys.addAll(tmp);
                    }

                    citys = (List) citys.stream().distinct().collect(Collectors.toList());//去重
                    //System.out.println(Arrays.toString(citys.toArray()));
                    criteria.subCriteria(new Criteria("content").in(citys).or("title").in(citys));
                }
            }
            else if(searchSplitArray.size()>1 && regionKeywordMatch ==0){
                List<String> citys = new ArrayList<>();
                for (String searchString : searchSplitArray){
                    List<Integer> codeid  = areaRepository.findCodeidByCityName(searchString);
                    for (Integer co:codeid){
                        List<String> tmp = areaRepository.findCityNameByCodeid(co) ;
                        for(int i=0;i<tmp.size();i++){
                            tmp.set(i,tmp.get(i).replaceAll("\\s*", ""));
                            if(tmp.get(i).contains("市辖")||tmp.get(i).contains("县辖")){
                                tmp.remove(i);
                            }
                        }
                        citys.addAll(tmp);
                    }
                    citys = (List) citys.stream().distinct().collect(Collectors.toList());//去重

                }
                //System.out.println(Arrays.toString(citys.toArray()));
                criteria.subCriteria(new Criteria("content").in(citys).or("title").in(citys));
            }


        }
        if (!cflag.isEmpty())
        {
            criteria.subCriteria(new Criteria().and("cflag").is(cflag));
        }
        if (!startPublishedDay.isEmpty() && !endPublishedDay.isEmpty())
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                Date startDate = sdf.parse(startPublishedDay);
                Date endDate = sdf.parse(endPublishedDay);
                criteria.subCriteria(new Criteria().and("publishedDay").between(startDate, endDate));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (!fromType.isEmpty())
        {
            String[] searchSplitArray1 = fromType.trim().split("\\s+");
            List<String>searchSplitArray = Arrays.asList(searchSplitArray1);

            if(searchSplitArray.size()>1){
                criteria.subCriteria(new Criteria().and("fromType").in(searchSplitArray));
            }else {
                criteria.subCriteria(new Criteria().and("fromType").is(searchSplitArray.get(0)));
            }

        }
        CriteriaQuery query = new CriteriaQuery(criteria);
        if (timeOrder == 0) {
            query.setPageable(PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "publishedDay")));
        }
        else {
            query.setPageable(PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "publishedDay")));
        }
        SearchHits<Data> searchHits = this.elasticsearchOperations.search(query, Data.class);
        SearchPage<Data> searchPage = SearchHitSupport.searchPageFor(searchHits, query.getPageable());
        long hitNumber = this.elasticsearchOperations.count(query, Data.class);

        List<Data> pageDataContent = new ArrayList<>();
        for (SearchHit<Data> hit : searchPage.getSearchHits())
        {
            pageDataContent.add(hit.getContent());
        }

        DataResponse result = new DataResponse();
        result.setHitNumber(hitNumber);
        result.setDataContent(pageDataContent);

        return result;
    }

    static  boolean flag = false;
    @Override
    public Set<String> sensitiveWordFiltering(String text){

        if(false==flag){
            // 初始化敏感词库对象
            SensitiveWordInit sensitiveWordInit = new SensitiveWordInit();
            // 从数据库中获取敏感词对象集合（调用的方法来自Dao层，此方法是service层的实现类）
            List<SensitiveWord> sensitiveWords = sensitiveWordRepository.findAll();
            // 构建敏感词库
            Map sensitiveWordMap = sensitiveWordInit.initKeyWord(sensitiveWords);
            // 传入SensitivewordEngine类中的敏感词库
            SensitivewordEngine.sensitiveWordMap = sensitiveWordMap;
            flag = true;
        }
        // 得到敏感词有哪些，传入2表示获取所有敏感词
        Set<String> set = SensitivewordEngine.getSensitiveWord(text, 2);
        return set;
    }
}
