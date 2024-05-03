
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 合同
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/hetong")
public class HetongController {
    private static final Logger logger = LoggerFactory.getLogger(HetongController.class);

    private static final String TABLE_NAME = "hetong";

    @Autowired
    private HetongService hetongService;


    @Autowired
    private TokenService tokenService;

    @Autowired
    private DictionaryService dictionaryService;//字典
    @Autowired
    private FangwuService fangwuService;//房屋
    @Autowired
    private FangwuCollectionService fangwuCollectionService;//房屋收藏
    @Autowired
    private FangwuLiuyanService fangwuLiuyanService;//房屋留言
    @Autowired
    private FangwuYuyueService fangwuYuyueService;//预约看房
    @Autowired
    private GonggaoService gonggaoService;//公告
    @Autowired
    private YonghuService yonghuService;//用户
    @Autowired
    private YuangongService yuangongService;//员工
    @Autowired
    private UsersService usersService;//管理员


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("用户".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        else if("员工".equals(role))
            params.put("yuangongId",request.getSession().getAttribute("userId"));
        params.put("hetongDeleteStart",1);params.put("hetongDeleteEnd",1);
        CommonUtil.checkMap(params);
        PageUtils page = hetongService.queryPage(params);

        //字典表数据转换
        List<HetongView> list =(List<HetongView>)page.getList();
        for(HetongView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        HetongEntity hetong = hetongService.selectById(id);
        if(hetong !=null){
            //entity转view
            HetongView view = new HetongView();
            BeanUtils.copyProperties( hetong , view );//把实体数据重构到view中
            //级联表 用户
            //级联表
            YonghuEntity yonghu = yonghuService.selectById(hetong.getYonghuId());
            if(yonghu != null){
            BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "yonghuId"
, "yuangongId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setYonghuId(yonghu.getId());
            }
            //级联表 员工
            //级联表
            YuangongEntity yuangong = yuangongService.selectById(hetong.getYuangongId());
            if(yuangong != null){
            BeanUtils.copyProperties( yuangong , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "yonghuId"
, "yuangongId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setYuangongId(yuangong.getId());
            }
            //级联表 房屋
            //级联表
            FangwuEntity fangwu = fangwuService.selectById(hetong.getFangwuId());
            if(fangwu != null){
            BeanUtils.copyProperties( fangwu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "yonghuId"
, "yuangongId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setFangwuId(fangwu.getId());
            }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody HetongEntity hetong, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,hetong:{}",this.getClass().getName(),hetong.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("用户".equals(role))
            hetong.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        else if("员工".equals(role)){
            hetong.setYuangongId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
            hetong.setYonghuTongyiTypes(1);
        }

        Wrapper<HetongEntity> queryWrapper = new EntityWrapper<HetongEntity>()
            .eq("yonghu_id", hetong.getYonghuId())
            .eq("yuangong_id", hetong.getYuangongId())
            .eq("fangwu_id", hetong.getFangwuId())
            .eq("hetong_name", hetong.getHetongName())
            .eq("hetong_address", hetong.getHetongAddress())
            .eq("hetong_types", hetong.getHetongTypes())
            .eq("yonghu_tongyi_types", hetong.getYonghuTongyiTypes())
            .eq("hetong_delete", hetong.getHetongDelete())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        HetongEntity hetongEntity = hetongService.selectOne(queryWrapper);
        if(hetongEntity==null){
            hetong.setHetongDelete(1);
            hetong.setInsertTime(new Date());
            hetong.setCreateTime(new Date());
            hetongService.insert(hetong);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody HetongEntity hetong, HttpServletRequest request) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.debug("update方法:,,Controller:{},,hetong:{}",this.getClass().getName(),hetong.toString());
        HetongEntity oldHetongEntity = hetongService.selectById(hetong.getId());//查询原先数据

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("用户".equals(role))
//            hetong.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
//        else if("员工".equals(role))
//            hetong.setYuangongId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        if("".equals(hetong.getHetongFile()) || "null".equals(hetong.getHetongFile())){
                hetong.setHetongFile(null);
        }

            hetongService.updateById(hetong);//根据id更新
            return R.ok();
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids, HttpServletRequest request){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        List<HetongEntity> oldHetongList =hetongService.selectBatchIds(Arrays.asList(ids));//要删除的数据
        ArrayList<HetongEntity> list = new ArrayList<>();
        for(Integer id:ids){
            HetongEntity hetongEntity = new HetongEntity();
            hetongEntity.setId(id);
            hetongEntity.setHetongDelete(2);
            list.add(hetongEntity);
        }
        if(list != null && list.size() >0){
            hetongService.updateBatchById(list);
        }

        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            List<HetongEntity> hetongList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            HetongEntity hetongEntity = new HetongEntity();
//                            hetongEntity.setYonghuId(Integer.valueOf(data.get(0)));   //用户 要改的
//                            hetongEntity.setYuangongId(Integer.valueOf(data.get(0)));   //员工 要改的
//                            hetongEntity.setFangwuId(Integer.valueOf(data.get(0)));   //房屋 要改的
//                            hetongEntity.setHetongUuidNumber(data.get(0));                    //合同编号 要改的
//                            hetongEntity.setHetongName(data.get(0));                    //合同名称 要改的
//                            hetongEntity.setHetongFile(data.get(0));                    //合同文件 要改的
//                            hetongEntity.setHetongAddress(data.get(0));                    //签订地点 要改的
//                            hetongEntity.setHetongTypes(Integer.valueOf(data.get(0)));   //合同类型 要改的
//                            hetongEntity.setQiandingTime(sdf.parse(data.get(0)));          //签订时间 要改的
//                            hetongEntity.setHetongContent("");//详情和图片
//                            hetongEntity.setYonghuTongyiTypes(Integer.valueOf(data.get(0)));   //是否同意 要改的
//                            hetongEntity.setHetongDelete(1);//逻辑删除字段
//                            hetongEntity.setInsertTime(date);//时间
//                            hetongEntity.setCreateTime(date);//时间
                            hetongList.add(hetongEntity);


                            //把要查询是否重复的字段放入map中
                                //合同编号
                                if(seachFields.containsKey("hetongUuidNumber")){
                                    List<String> hetongUuidNumber = seachFields.get("hetongUuidNumber");
                                    hetongUuidNumber.add(data.get(0));//要改的
                                }else{
                                    List<String> hetongUuidNumber = new ArrayList<>();
                                    hetongUuidNumber.add(data.get(0));//要改的
                                    seachFields.put("hetongUuidNumber",hetongUuidNumber);
                                }
                        }

                        //查询是否重复
                         //合同编号
                        List<HetongEntity> hetongEntities_hetongUuidNumber = hetongService.selectList(new EntityWrapper<HetongEntity>().in("hetong_uuid_number", seachFields.get("hetongUuidNumber")).eq("hetong_delete", 1));
                        if(hetongEntities_hetongUuidNumber.size() >0 ){
                            ArrayList<String> repeatFields = new ArrayList<>();
                            for(HetongEntity s:hetongEntities_hetongUuidNumber){
                                repeatFields.add(s.getHetongUuidNumber());
                            }
                            return R.error(511,"数据库的该表中的 [合同编号] 字段已经存在 存在数据为:"+repeatFields.toString());
                        }
                        hetongService.insertBatch(hetongList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }




    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        CommonUtil.checkMap(params);
        PageUtils page = hetongService.queryPage(params);

        //字典表数据转换
        List<HetongView> list =(List<HetongView>)page.getList();
        for(HetongView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段

        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        HetongEntity hetong = hetongService.selectById(id);
            if(hetong !=null){


                //entity转view
                HetongView view = new HetongView();
                BeanUtils.copyProperties( hetong , view );//把实体数据重构到view中

                //级联表
                    YonghuEntity yonghu = yonghuService.selectById(hetong.getYonghuId());
                if(yonghu != null){
                    BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setYonghuId(yonghu.getId());
                }
                //级联表
                    YuangongEntity yuangong = yuangongService.selectById(hetong.getYuangongId());
                if(yuangong != null){
                    BeanUtils.copyProperties( yuangong , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setYuangongId(yuangong.getId());
                }
                //级联表
                    FangwuEntity fangwu = fangwuService.selectById(hetong.getFangwuId());
                if(fangwu != null){
                    BeanUtils.copyProperties( fangwu , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setFangwuId(fangwu.getId());
                }
                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody HetongEntity hetong, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,hetong:{}",this.getClass().getName(),hetong.toString());
        Wrapper<HetongEntity> queryWrapper = new EntityWrapper<HetongEntity>()
            .eq("yonghu_id", hetong.getYonghuId())
            .eq("yuangong_id", hetong.getYuangongId())
            .eq("fangwu_id", hetong.getFangwuId())
            .eq("hetong_uuid_number", hetong.getHetongUuidNumber())
            .eq("hetong_name", hetong.getHetongName())
            .eq("hetong_address", hetong.getHetongAddress())
            .eq("hetong_types", hetong.getHetongTypes())
            .eq("yonghu_tongyi_types", hetong.getYonghuTongyiTypes())
            .eq("hetong_delete", hetong.getHetongDelete())
//            .notIn("hetong_types", new Integer[]{102})
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        HetongEntity hetongEntity = hetongService.selectOne(queryWrapper);
        if(hetongEntity==null){
            hetong.setHetongDelete(1);
            hetong.setInsertTime(new Date());
            hetong.setCreateTime(new Date());
        hetongService.insert(hetong);

            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

}

