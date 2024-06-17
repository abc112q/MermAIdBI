技术栈：AIGC+SpringBoot+MyBatis-Plus+Redis+RabbitMQ
项目介绍: 智能数据分析平台，用户通过导入数据集，并输入分析需求，自动生成可视化图表及分析结论
- 后端自定义Prompt模板和封装用户数据诉求，最终生成可视化图表的JSON数据供前端渲染
- 为优化AIGC接口的Token限制，使用Easy Excel解析用户上传的xslx文件并压缩为csv文件，以提升用户可输入的数据量
- 为防止用户恶意占用系统资源，基于Redisson的RateLimiter实现分布式限流，控制单个用户的访问频率
- 使用Reids缓存用户图表数据，并解决缓存穿透、缓存雪崩等问题，提高图表查询效率
- 由于AIGC的响应时间较长，基于自定义线程池+任务队列实现了AIGC的并发执行和异步化，提交任务后即可给出响应
- 由于本地任务队列重启会丢失数据，使用RabbitMQ来接收并持久化任务消息，将图表生成与系统解耦，提高系统的可靠性
- 通过消息队列的重试机制来处理AI生成失败的图表数据，并将重试仍然失败或超时的消息放入死信队列，便于人工处理

  ![Image text](https://github.com/abc112q/mermAIdBI/edit/master/img/img.png)
  ![Image text](https://github.com/abc112q/mermAIdBI/edit/master/img/img_1.png)
  ![Image text](https://github.com/abc112q/mermAIdBI/edit/master/img/img_2.png)
