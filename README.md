# Relumin (Redis cluster admin)

## What's Relumin ?

Relumin is a REdis cLUster adMIN tool.

Main feature is ...

* Visualize Redis cluster status
* Operating Cluster
    * Add node, Resharding, Delete node, Replicate, Failover...etc
* Visualize Redis node metrics as chart
    * Metrics is get by http://redis.io/commands/info
* Alert
    * You can set threshold freely.

### Why develop ?

* There are some redis tool, but they don't support Redis cluster.
* It is difficult to understand cluster status. I want visualize.
* Other cluster storages have GUI tool.  
    * For example,  
        * Riak has "Riak control(http://docs.basho.com/riak/latest/ops/advanced/riak-control/)".
        * Cassandra has "Datastax(http://www.datastax.com/)".

So I developed.

### Screenshot

Cluster status
![SS1](https://raw.githubusercontent.com/be-hase/relumin/develop/docs/img/SS1.png)

Metics chart
![SS2](https://raw.githubusercontent.com/be-hase/relumin/develop/docs/img/SS2.png)

Setting alert
![SS3](https://raw.githubusercontent.com/be-hase/relumin/develop/docs/img/SS3.png)

## Getting start

Very easy.

### 1. Download JAR file

Please download from this.  
https://github.com/be-hase/relumin/releases

### 2. Install Java8 on your server

Install Java8 on your server.  
( I tested Oracle JDK. )

### 3. Install Redis(not cluster) on your server

Install Redis on your server.

Redis is used for store meta data and metrics data.

Memory is expensive.
So I will support MySQL. Prease wait...

### 4. Write "relumin.yml" (config file)

Please write config file.  
Specify this config path when you start Relumin.

Example:
```yaml
host: 10.10.10.10

server:
  port: 8080
  monitorPort: 20080

redis:
  host: localhost
  port: 10000

auth:
  enabled: true
  allowAnonymous: true

notice:
  mail:
    host: smtp.hoge.com
    port: 25
    from: relumin-alert@hoge.com

scheduler:
  collectStaticsInfoMaxCount: 10000
```

This is property table.

| property name | default | required | description |
| --- | --- | --- | --- |
| host |  |  | Host or IP of server which Relumin running on. <br> This is used for alert mail. |
| server.port | 8080 |  | Port of Relumin server. |
| server.monitorPort | 20080 |  | Port of Relumin server for monitoring. |
| redis.prefixKey | _relumin |  | Prefix key of Redis. This prefix key is used for meta data and metrics data. |
| redis.host |  | Y | Redis's host. |
| redis.port |  | Y | Redis's port |
| auth.enabled | false |  | If this value is false, Every user can execute all operation. <br> (Cluster operation, User CRUD, Notification CRUD...etc)  |
| auth.allowAnonymous | false |  | If this value is true, anonymous user can access Relumin. <br> But only view. |
| notice.mail.host |  |  | Host of SMTP server. |
| notice.mail.port |  |  | Port of SMTP server. |
| notice.mail.user |  |  | User of SMTP Auth. |
| notice.mail.password |  |  | Password of SMTP Auth. |
| notice.mail.charset | UTF-8 |  | Charset of alert mail. |
| notice.mail.from |  |  | "From" mail address. This value can be overriden by cluster setting. |
| scheduler.refreshClustersIntervalMillis | 120000 |  | Interval(milliseconds) of refresh meta data of Redis cluster. <br> Recommend default. |
| scheduler.collectStaticsInfoIntervalMillis | 120000 |  | Interval(milliseconds) of collect metrics data. If threshold condition is satisfied, send notice. |
| scheduler.collectStaticsInfoMaxCount | 1500 |  | Save count of metrics data. <br> If you want save latest 1 week, you specify "5040". <br><br> (60 * 24 * 7) / 2 = 5040 |
| outputMetrics.fluentd.enabled | false |  | If you want to forward metris data into fluentd, specify true. |
| outputMetrics.fluentd.host |  |  | Host of Fluentd. |
| outputMetrics.fluentd.port |  |  | Port of Fluentd. |
| outputMetrics.fluentd.timeout | 3000 |  | Timeout of Fluentd Java Library. |
| outputMetrics.fluentd.bufferCapacity | 1048576 |  | BufferCapacity of Fluentd Java Library. |
| outputMetrics.fluentd.tag | relumin |  | Tag of Fluentd. <br> Send by below format. <br> `<outputMetrics.fluentd.tag>.outputMetrics.fluentd.nodeTag>` |
| outputMetrics.fluentd.nodeTag | node |  | Ref avobe. |

### 5. Start Jar (FINISH !!)

Please start jar file.

You can specify log parameter.

* log.type : console or file
* log.level : DEBUG, INFO, WARN, ERROR... (logback's level)
* log.dir : /path/your/logDir

```bash
java -Dconfig=/path/your/config/relumin.yml -Dlog.type=file -Dlog.level=INFO -Dlog.dir=/path/your/logDir -jar relumin-0.0.1.jar > /dev/null 2>&1 &
```

Relumin run on JVM, so you can specify JVM option. Like a below.

```bash
JVM_OPT="-server -Dfile.encoding=UTF-8 -Xms2g -Xmx2g \
-XX:SurvivorRatio=8 -XX:TargetSurvivorRatio=80 \
-XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled \
-XX:CMSInitiatingOccupancyFraction=70 \
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOG_PATH}/relumin -verbose:gc \
-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:${LOG_PATH}/relumin/relumin.gc.log \
-XX:+UseGCLogFileRotation -XX:GCLogFileSize=512m -XX:NumberOfGCLogFiles=5"

java -Dconfig=/path/your/config/relumin.yml -Dlog.type=file -Dlog.level=INFO -Dlog.dir=/path/your/logDir -jar relumin-0.0.1.jar > /dev/null 2>&1 &
```

## Authenticate and Authorize (Option)

Relumin support role-based authenticate&authorize.

### Role

Relumin user has 2 role.

| role name | description |
| --- | --- |
| VIEWER | Only view user. |
| RELUMIN_ADMIN | This user execute all Relumin operation. <br> (Cluster operation, User CRUD, Notification CRUD...etc) |

### About adding user

A user who is RELUMIN_ADMIN can add user from 'http://<relumin host:port>/#/users'.

How do I add First RELUMIN_ADMIN user?  
Please specify 'auth.enabled' false, and add RELUMIN_USER, and 'auth.enabled' true.

### Anonymous user

You can configure whether anonymous user can access Relumin or not by specifying 'auth.allowAnonymous'.
If this value is true, anonymous user can use relumin as VIEWER role.

## Contribute

I'm now prepare for test environment. (Travis CI or Circle CI).  
(Currentry test execute on my local vagrant env.)

Please wait...

## Milestone (Maybe...)

* Support share URL
* Support MySQL for meta data and metrics data.

## Appendix

### Architecture

**Server**

* Written by Java.
* Based on Spring Boot.

**Front**

* Written by pure javascript.
* Based on React(Flux), Webpack and Gulp.
