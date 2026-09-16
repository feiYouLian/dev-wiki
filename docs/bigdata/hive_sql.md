### hive sql

<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [hive sql](#hive-sql)
  - [DDL](#ddl)
  - [数据类型](#数据类型)
  - [DML](#dml)
  - [查询](#查询)
  - [函数](#函数)
  - [逻辑运算](#逻辑运算)
  - [关系运算](#关系运算)
  - [LIKE](#like)
  - [REGEXP](#regexp)
  - [数学运算](#数学运算)
  - [条件函数](#条件函数)
  - [数值函数](#数值函数)
  - [日期函数](#日期函数)
  - [string 函数](#string-函数)
  - [struct 函数](#struct-函数)

<!-- /code_chunk_output -->

[参考资料](https://segmentfault.com/a/1190000039760261)

#### DDL

```sql
-- 创建表
create table if not exists school(id int, name string, address string);
-- row format delimited fields terminated by ','  指定字段分隔符，默认分隔符为 '\001'
-- stored as 指定存储格式
-- location 指定存储位置;
create table student(id int,name string,age int) partitioned by (school string) row format delimited fields terminated by ',' stored as textfile location '/user/student';


create table stu2 as select * from student;

create table stu3 like student;

-- 查看表
desc student;
describe formatted student;
-- 查看 分区
show partitions student;

-- 修改表
alter table stu2 rename to stu_new;
alter table stu_new add columns (col1 string, col2 string);
alter table stu_new change column col1 sex int;

alter table stu_new add partition(school='xiaoxue') partition(school = 'zhongxue');

alter table stu_new drop partition(school='daxue');

-- 删除表
drop table stu_new;

-- 清空表 慎用，不可恢复
truncate table stu_new;
```

#### 数据类型

- hive 建表时候的字段类型:

| **分类** | **类型**   | **描述**                                         | **字面量示例**                                                  |
| -------- | ---------- | ------------------------------------------------ | --------------------------------------------------------------- |
| 原始类型 | BOOLEAN    | true/false                                       | TRUE                                                            |
|          | TINYINT    | 1 字节的有符号整数 -128~127                      | 1Y                                                              |
|          | SMALLINT   | 2 个字节的有符号整数，-32768~32767               | 1S                                                              |
|          | **INT**    | 4 个字节的带符号整数                             | 1                                                               |
|          | BIGINT     | 8 字节带符号整数                                 | 1L                                                              |
|          | FLOAT      | 4 字节单精度浮点数 1.0                           |                                                                 |
|          | DOUBLE     | 8 字节双精度浮点数                               | 1.0                                                             |
|          | DEICIMAL   | 任意精度的带符号小数                             | 1.0                                                             |
|          | **STRING** | 字符串，变长                                     | “a”,’b’                                                         |
|          | VARCHAR    | 变长字符串                                       | “a”,’b’                                                         |
|          | CHAR       | 固定长度字符串                                   | “a”,’b’                                                         |
|          | BINARY     | 字节数组                                         | 无法表示                                                        |
|          | TIMESTAMP  | 时间戳，毫秒值精度                               | 122327493795                                                    |
|          | **DATE**   | 日期                                             | ‘2016-03-29’                                                    |
|          | INTERVAL   | 时间频率间隔                                     |                                                                 |
| 复杂类型 | ARRAY      | 有序的的同类型的集合                             | array(1,2)                                                      |
|          | MAP        | key-value,key 必须为原始类型，value 可以任意类型 | map(‘a’,1,’b’,2)                                                |
|          | STRUCT     | 字段集合,类型可以不同                            | struct(‘1’,1,1.0), named_stract(‘col1’,’1’,’col2’,1,’clo3’,1.0) |
|          | UNION      | 在有限取值范围内的一个值                         | create_union(1,’a’,63)                                          |

> **对 decimal 类型简单解释下**：  
> 用法：decimal(11,2) 代表最多有 11 位数字，其中后 2 位是小数，整数部分是 9 位；如果整数部分超过 9 位，则这个字段就会变成 null；如果小数部分不足 2 位，则后面用 0 补齐两位，如果小数部分超过两位，则超出部分四舍五入  
> 也可直接写 decimal，后面不指定位数，默认是 decimal(10,0) 整数 10 位，没有小数

#### DML

hive 不支持 行级修改，所以没有 update 和 delete 操作，
一般为 覆盖（overwrite）或 追加（append）

```sql
-- /opt/hive/student.txt
1,zhangsan,10
2,lisi,10
3,zhangsan,10
4,lisi,8
5,wangwu
```

```sql
-- local : 表示本地文件，去掉local表示是hdfs文件
-- overwrite：表示覆盖原来数据， 去掉表示 追加数据

load data local inpath '/opt/hive/student.txt' overwrite into table student partition (school = 'xiaoxue');

insert into table student partition(school = 'zhongxue') values (6,'xiaoliu',15),(7,'aqi',16);

```

#### 查询

```sql
SELECT [ALL | DISTINCT] select_expr, select_expr, ...
FROM table_reference
[WHERE where_condition]
[GROUP BY col_list [HAVING condition]]
[CLUSTER BY col_list
| [DISTRIBUTE BY col_list] [SORT BY| ORDER BY col_list]
]
[LIMIT number]
```

```sql
select * from student;

-- where
select * from student where age > 5 and (school = 'xiaoxue' or name = 'zhangsan') ;

-- order by 全局排序，所以最后只有一个reduce，也就是在一个节点执行，如果数据量太大，就会耗费较长时间
select * from student order by age;

-- sort by 不是全局排序，其在数据进入reducer前完成排序。每个MapReduce内部进行排序，对全局结果集来说不是排序。
select * from student sort by age;

-- group by
select name,count(id) countNum from student group by name having countNum > 1 ;

-- join
-- INNER JOIN 内连接：只有进行连接的两个表中都存在与连接条件相匹配的数据才会被保留下来

select * from  student stu join school sch on sch.name = stu.school and sch.id>1; -- inner 可省略

--  LEFT OUTER JOIN 左外连接：左边所有数据会被返回，右边符合条件的被返回
select * from student stu left join school sch on sch.name = stu.school and sch.id>1; -- outer可省略

-- RIGHT OUTER JOIN 右外连接：右边所有数据会被返回，左边符合条件的被返回
select * from student stu right join school schu on sch.name = stu.school and sch.id>1; -- outer可省略

-- FULL OUTER JOIN 满外(全外)连接: 将会返回所有表中符合条件的所有记录。如果任一表的指定字段没有符合条件的值的话，那么就使用NULL值替代
select * from student stu full join school sch on sch.name = stu.school; -- outer可省略。

```

#### 函数

hive 支持 count(),max(),min(),sum(),avg() 等常用的聚合函数

#### 逻辑运算

支持：逻辑与(and)、逻辑或(or)、逻辑非(not)

#### 关系运算

支持：等值(=)、不等值(!= 或 <>)、小于(<)、小于等于(<=)、大于(>)、大于等于(>=) 空值判断(is null)、非空判断(is not null)、 包含 (in)

#### LIKE

```sql
-- from tableName 可省略
select 1 from tableName where 'abcd' LIKE '_b%'; -- 1
```

#### REGEXP

```sql
-- 与 RLIKE 功能一致, from tableName 可省略
select 1 from tableName where 'footbar' REGEXP '^f.*r$'; -- 1
```

#### 数学运算

支持所有数值类型：加(+)、减(-)、乘(\*)、除(/)、取余(%)、位与(&)、位或(|)、位异或(^)、位取反(~)

#### 条件函数

```sql
-- if 函数
-- if(boolean testCondition, T valueTrue, T valueFalseOrNull)
select if(1=2,100,200) ; -- 200
select if(1=1,100,200) ; -- 100

-- coalesce 非空查找函数
-- coalesce(T v1, T v2, …)
select coalesce(null,'100','50') ;-- 100
select coalesce(null,null,'50') ;-- 50
select coalesce(null,null,null) ;-- null

-- case when 条件判断函数
-- case when a then b [when c then d]* [else e] end
select case when 1=2 then 'tom' when 2=2 then 'mary' else 'tim' end ; -- mary
-- case a when b then c [when d then e]* [else f] end
select case 100 when 50 then 'tom' when 100 then 'mary' else 'tim' end; -- mary
```

#### 数值函数

```sql
-- round 取整函数(四舍五入)
-- round(double a)
select round(3.1415926); --3

-- round(double a, int d)
select round(3.1415926,4); -- 3.1416

-- floor 向下取整
-- floor(double a)
select floor(3.641); -- 3

-- ceil 向上取整
-- ceil(double a)
select ceil(3.1415926); -- 4

-- rand 取随机数函数
-- rand(),rand(int seed)
 select rand(); -- 0.5577432776034763，每次执行结果都不同
 select rand(100) ; -- 0.7220096548596434 只要指定种子，每次执行此语句得到的结果一样的

-- pow幂运算函数
-- pow(double a, double p)
select pow(2,4); -- 16.0

-- bin 二进制函数
-- sqrt 开平方
-- exp 自然指数函数
-- log10 以10为底对数函数
```

#### 日期函数

```sql
-- unix_timestamp 获取当前UNIX时间戳函数
-- unix_timestamp()
select unix_timestamp();

-- from_unixtime  UNIX时间戳转日期函数
-- from_unixtime(bigint unixtime[, string format])
select from_unixtime(1616906976,'yyyyMMdd'); -- 20210328

-- unix_timestamp  日期转UNIX时间戳函数
-- unix_timestamp(string date)
select unix_timestamp('2021-03-08 14:21:15'); -- 1615184475

-- unix_timestamp(string date, string pattern)
select unix_timestamp('2021-03-08 14:21:15','yyyyMMdd HH:mm:ss') -- 1615184475

-- to_date  日期时间转日期函数
-- to_date(string timestamp)
select to_date('2021-03-28 14:03:01') -- 2021-03-28

-- year 日期转年函数
-- year(string date)
select year('2021-03-28 10:03:01') -- 2021
-- month day  hour minute second weekofyear  同 year

-- datediff  日期比较函数
-- datediff(string enddate, string startdate)
select datediff('2020-12-08','2012-05-09'); -- 213

-- date_add  日期增加函数
-- date_add(string startdate, int days)
select date_add('2020-12-08',10) -- 2020-12-18

-- date_sub  日期减少函数 同 date_add

```

#### string 函数

```sql

-- length 长度函数
-- length(string A)
select length('abcedfg'); -- 7

-- reverse 反转函数
-- reverse(string A)
select reverse('abcedfg') -- gfdecba


-- concat 连接函数：
-- concat(string A, string B…)
select concat('abc','def','gh'); -- abcdefgh

-- concat_ws  带分隔符连接函数
-- concat_ws(string SEP, string A, string B…)
select concat_ws(',','abc','def','gh') -- abc,def,gh

-- substr,substring  截取函数
-- substr(string A, int start),substring(string A, int start)
select substr('abcde',3) -- cde
select substring('abcde',3) -- cde
select substr('abcde',-1) -- e

-- substr(string A, int start, int len),substring(string A, int start, int len)
select substr('abcde',3,2) -- cd
select substring('abcde',3,2) -- cd
select substring('abcde',-2,2) -- de

-- upper,ucase  转大写函数
-- upper(string A) ucase(string A)
select upper('abSEd') -- ABSED

-- lower,lcase  转小写函数
-- lower(string A) lcase(string A)
select lower('abSEd'); -- absed

-- trim  去空格函数
-- trim(string A)
 select trim(' abc '); -- abc

-- ltrim 左边去空格函数
-- rtrim 右边去空格函数

-- regexp_replace  正则表达式替换函数
-- regexp_replace(string A, string B, string C)
select regexp_replace('foobar', 'oo|ar', ''); -- fb

-- regexp_extract 正则表达式解析函数
-- regexp_extract(string subject, string pattern, int index)
select regexp_extract('foothebar', 'foo(.*?)(bar)', 1)  -- foo
select regexp_extract('foothebar', 'foo(.*?)(bar)', 2) -- bar
select regexp_extract('foothebar', 'foo(.*?)(bar)', 0)  -- foobar

-- parse_url  URL解析函数
-- parse_url(string urlString, string partToExtract [, string keyToExtract])
-- partToExtract的有效值为：HOST, PATH, QUERY, REF, PROTOCOL, AUTHORITY, FILE, and USERINFO.
select parse_url ('https://www.tableName.com/pat...', 'HOST') -- www.tableName.com

-- get_json_object  json解析函数
-- get_json_object(string json_string, string path)
select get_json_object('{"store":{"fruit":[{"weight":8,"type":"apple"},{"weight":9,"type":"pear"}], "bicycle":{"price":19.95,"color":"red"} },"email":"amy@only_for_json_udf_test.net","owner":"amy"}','$.owner') -- amy

-- space  空格字符串函数
-- space(int n)
select length(space(10)); -- 10

-- repeat  重复字符串函数
-- repeat(string str, int n)
select repeat('abc',5);-- abcabcabcabcabc

-- ascii 首字符ascii函数
-- ascii(string str)
select ascii('abcde'); -- 97

-- lpad 左补足函数
-- lpad(string str, int len, string pad)
select lpad('abc',10,'td'); -- tdtdtdtabc

-- rpad 右补足函数 同上

-- split 分割字符串函数
-- split(string str, string pat)
select split('abtcdtef','t') -- ["ab","cd","ef"]

-- find_in_set 集合查找函数
-- find_in_set(string str, string strList)
select find_in_set('ab','ef,ab,de') -- 2
select find_in_set('at','ef,ab,de') -- 0


```

#### struct 函数

```sql
-- cast 类型转换函数
-- cast(expr as <type>)
select cast('1' as bigint); -- 1

```
