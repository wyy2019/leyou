package com.leyou.item.mapper;

import com.leyou.item.pojo.Category;
import tk.mybatis.mapper.additional.idlist.SelectByIdListMapper;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @author shkstart
 * @create 2022-04-1522:15
 */
public interface CategoryMapper extends Mapper<Category>, SelectByIdListMapper<Category, Long> {

}