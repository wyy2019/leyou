package com.leyou.search.client;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.item.pojo.Spu;
import com.leyou.search.pojo.Goods;
import com.leyou.search.reponsitory.GoodsReponsitory;
import com.leyou.search.service.SearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaAutoServiceRegistration;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ElasticsearchTest {

    @Autowired
    private GoodsReponsitory goodsReponsitory;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private SearchService searchService;

    @Autowired
    private GoodsClient goodsClient;

    @MockBean
    private EurekaAutoServiceRegistration eurekaAutoServiceRegistration;

    @Test
    public void test() {
        this.elasticsearchTemplate.createIndex(Goods.class);
        this.elasticsearchTemplate.putMapping(Goods.class);

        Integer page = 1;
        Integer rows = 100;

        do {
            // 分页查询spu,获取分页结果集
            PageResult<SpuBo> result = this.goodsClient.querySpuBoByPage(null, null, page, rows);
            // 获取当前页的数据
            List<SpuBo> items = result.getItems();
            // 处理List<SpuBo> ==> List<Goods>
            List<Goods> googsList = items.stream().map(spuBo -> {
                try {
                    return this.searchService.buildGoods(spuBo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }).collect(Collectors.toList());

            // 执行新增数据的方法
            this.goodsReponsitory.saveAll(googsList);
            page++;
            rows = items.size();
        } while (rows == 100);
    }

    @Test
    public void createIndex(){
        // 创建索引
        this.elasticsearchTemplate.createIndex(Goods.class);
        // 配置映射
        this.elasticsearchTemplate.putMapping(Goods.class);
        Integer page = 1;
        Integer rows = 185;

        do {
            // 分批查询spuBo
            PageResult<SpuBo> pageResult = this.goodsClient.querySpuBoByPage(null, true, page, rows);
            // 遍历spubo集合转化为List<Goods>
            List<Goods> goodsList = pageResult.getItems().stream().map(spuBo -> {
                try {
                    return this.searchService.buildGoods((Spu) spuBo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }).collect(Collectors.toList());
            this.goodsReponsitory.saveAll(goodsList);

            // 获取当前页的数据条数，如果是最后一页，没有100条
            rows = pageResult.getItems().size();
            // 每次循环页码加1
            page++;
        } while (rows == 185);
    }


    @Test
    public void createIndex1(){
        // 创建索引库，以及映射
        this.elasticsearchTemplate.createIndex(Goods.class);
        this.elasticsearchTemplate.putMapping(Goods.class);
    }
}