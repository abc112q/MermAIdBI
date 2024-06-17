package com.aibi.springbootinit.manager;

import com.aibi.springbootinit.common.ErrorCode;
import com.aibi.springbootinit.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 专门提供RedisLimiter限流基础服务的
 *
 */

//Manager与具体的业务场景无关，类似于一个工具类，提供一些通用的基础服务
@Service
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    public void doRateLimit(String key){ //key用于区分不同的限流器，比如不同的用户id应该分别统计

        //创建一个名称为user_limiter的限流器，每秒最多访问两次
        RRateLimiter rateLimiter=redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL,2,1, RateIntervalUnit.SECONDS);

        //每当来了一个操作后，请求一个令牌
        boolean canOp=rateLimiter.tryAcquire(1);
        //这里可以判断一下，设置会员请求一次花费的令牌更少，二普通用户请求一次消耗的令牌多
        if(!canOp){
            //没有拿到令牌，不能执行操作
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
    }
}
