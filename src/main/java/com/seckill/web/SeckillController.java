package com.seckill.web;

import com.seckill.dto.Exposer;
import com.seckill.dto.SeckillExcution;
import com.seckill.dto.SeckillResult;
import com.seckill.entity.Seckill;
import com.seckill.enums.seckillStateEnum;
import com.seckill.exception.RepeatKillException;
import com.seckill.exception.SeckillCloseException;
import com.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/seckill")
public class SeckillController {
    private final Logger logger= LoggerFactory.getLogger(this.getClass());
    @Autowired
    private SeckillService seckillService;

    /**
     * 详情页列表
     * @param model
     * @return
     */
    @RequestMapping(value="/list",method = RequestMethod.GET)
    public String list(Model model){
        //获取列表页
        List<Seckill> seckillList = seckillService.getSeckillList();
        model.addAttribute("list",seckillList);
        return "list";
    }

    /**
     * 根据id查找详情页
     * @param seckillId
     * @param model
     * @return
     */
    @RequestMapping(value = "/{seckillId}/detail",method = RequestMethod.GET)
    public String detail(@PathVariable("seckillId") Long seckillId, Model model){
            if(seckillId==null){
                return "redirect:/seckillId/list";
            }
        Seckill seckill = seckillService.getById(seckillId);
            if(seckill==null){
                return "forward:/seckill/list";
            }
            model.addAttribute("seckill",seckill);
        return "detail";
    }

    /**
     * 暴露秒杀接口
     * @param seckillId
     * @return
     */
    //ajax json
    @RequestMapping(value = "/{seckillId}/exposer",method = RequestMethod.POST,produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<Exposer> exporser(@PathVariable("seckillId") Long seckillId){
        SeckillResult<Exposer> result;
        try{
            Exposer exposer = seckillService.exportSeckillUrl(seckillId);
            result=new SeckillResult<Exposer>(true,exposer);
        }catch (Exception e){
            logger.error(e.getMessage());
            result=new SeckillResult<Exposer>(false,e.getMessage());
        }
        return  result;
    }

    /**
     * 执行秒杀
     * @param seckillId
     * @param md5
     * @param userPhone
     * @return
     */
    @RequestMapping(value = "/{seckillId}/{md5}/execution",method = RequestMethod.POST,produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<SeckillExcution> execute(@PathVariable("seckillId") Long seckillId,@PathVariable("md5") String md5,
                                                  @CookieValue(value = "userPhone",required = false) Long userPhone){
        if(userPhone==null){
            return new SeckillResult<SeckillExcution>(false,"未注册");
        }
        try{
            SeckillExcution excution = seckillService.executeSeckill(seckillId,userPhone,md5);
            return new SeckillResult<SeckillExcution>(true,excution);
        }catch (RepeatKillException e1){
            SeckillExcution seckillExcution = new SeckillExcution(seckillId, seckillStateEnum.REPEAT_KILL);
            return new SeckillResult<SeckillExcution>(false,seckillExcution);
        }catch (SeckillCloseException e2){
            SeckillExcution seckillExcution = new SeckillExcution(seckillId, seckillStateEnum.END);
            return new SeckillResult<SeckillExcution>(false,seckillExcution);
        }catch (Exception e3){
            logger.error(e3.getMessage());
            SeckillExcution seckillExcution = new SeckillExcution(seckillId, seckillStateEnum.INNER_ERROR);
            return new SeckillResult<SeckillExcution>(false,seckillExcution);
        }
    }
    /**
     * 获取系统时间
     */
    @RequestMapping(value = "/time/now",method = RequestMethod.GET)
    @ResponseBody
    public SeckillResult<Long> time(){
        Date date = new Date();
        return new SeckillResult<Long>(true,date.getTime());
    }
}
