# MySQL Explain 执行计划分析（总结）

> 原文：[领导让下班前把 explain 画成一张图](https://juejin.cn/post/7478888679231193125)
> 作者：后端程序员 Aska · 发布于 2025-03-07
> 本文为该文章的要点提炼，便于速查与复习。

## 一、Explain 是什么

`EXPLAIN` 关键字可以**模拟优化器执行 SQL 语句**，分析查询语句或结构的性能瓶颈。

- 用法：在 `SELECT` 语句前加上 `EXPLAIN`，MySQL 会在查询上打一个标记，返回**执行计划**而不是真正执行 SQL。
- 例外：`FROM` 中的子查询仍会被执行，结果放入临时表。
- 输出：查询涉及几个表就输出几行（JOIN 多表会输出多行）。

官方文档参考：<https://dev.mysql.com/doc/refman/5.7/en/explain-output.html>

## 二、两个变种

| 变种 | 说明 |
| --- | --- |
| `EXPLAIN EXTENDED` | 在普通 explain 基础上额外提供优化信息；紧随其后的 `SHOW WARNINGS` 可看到优化器改写后的语句。多了 `filtered` 列（百分比），`rows * filtered / 100` 可估算与上一表连接的行数。 |
| `EXPLAIN PARTITIONS` | 比普通 explain 多了 `partitions` 字段，分区表查询时显示将访问的分区。 |

## 三、EXPLAIN 输出列详解

### 1. id 列
`SELECT` 的序列号，有几个 `SELECT` 就有几个 id。
- **id 越大，执行优先级越高**；
- id 相同则从上往下执行；
- id 为 `NULL` 最后执行。

### 2. select_type 列
表示对应行是简单还是复杂查询：

- `SIMPLE`：简单查询，不含子查询和 `UNION`；
- `PRIMARY`：复杂查询中最外层的 `SELECT`；
- `SUBQUERY`：出现在 `SELECT` 中的子查询（不在 `FROM` 子句）；
- `DERIVED`：`FROM` 子句中的子查询，结果存入临时表；
- `UNION`：`UNION` 中第二个及之后的 `SELECT`。

### 3. table 列
该行正在访问哪个表。子查询时显示 `<derivedN>`（依赖 id=N 的查询，先执行它）；`UNION RESULT` 的 table 为 `<unionM,N>`，表示参与 union 的 select 行 id。

### 4. type 列（访问类型，最重要）
表示 MySQL 如何查找表中的行。**从最优到最差**：

```
system > const > eq_ref > ref > range > index > ALL
```

一般要求查询**至少达到 `range` 级别，最好达到 `ref`**。

| 类型 | 含义 / 触发场景 |
| --- | --- |
| `NULL` | 优化阶段已分解语句，执行阶段无需访问表/索引（如 `SELECT MIN(id) FROM film`）。 |
| `const` / `system` | 主键或唯一键的所有列与常数比较，最多匹配一行，`system` 是 `const` 的特例（表只有一行匹配）。 |
| `eq_ref` | 连接时使用主键或唯一键索引的全部部分，最多返回一条记录，常见于 `JOIN`。 |
| `ref` | 使用普通索引或唯一索引的前缀，可能匹配多行（如 `WHERE name='film1'`）。 |
| `range` | 范围扫描，出现在 `IN()`、`BETWEEN`、`>`、`>=` 等操作中（`WHERE id > 1`）。 |
| `index` | 扫描全二级索引叶子节点，通常比 `ALL` 快（如 `SELECT * FROM film`，走 `idx_name`）。 |
| `ALL` | 全表扫描（聚簇索引全叶子），通常需要加索引优化。 |

### 5. possible_keys 列
查询**可能**使用的索引。可能出现有值但 `key` 为 `NULL` 的情况（数据太少，优化器认为索引帮助不大，选了全表扫描）。为空则无相关索引，可检查 `WHERE` 子句是否能建索引。

### 6. key 列
MySQL **实际采用**的索引。为 `NULL` 表示没用索引。可用 `FORCE INDEX` / `IGNORE INDEX` 强制或忽略。

### 7. key_len 列
索引中使用的字节数，可推算用到了索引的哪些列。计算规则：

- **字符串**：`char(n)` 存汉字为 `3n` 字节；`varchar(n)` 存汉字为 `3n + 2` 字节（2 字节存长度）。5.0.3 后 n 指字符数；utf-8 下数字/字母 1 字节、汉字 3 字节。
- **数值**：`tinyint` 1、`smallint` 2、`int` 4、`bigint` 8 字节。
- **时间**：`date` 3、`timestamp` 4、`datetime` 8 字节。
- 字段允许 `NULL` 需额外 1 字节；索引最大长度 768 字节，过长时做左前缀处理。

> 例：`idx_film_actor_id(film_id, actor_id)` 两个 int 各 4 字节，`key_len=4` 说明只用了 `film_id`。

### 8. ref 列
在 `key` 索引中，表查找所用到的列或常量，常见 `const`（常量）或字段名（如 `film.id`）。

### 9. rows 列
MySQL **预估**要读取并检测的行数（不是结果集行数）。

### 10. Extra 列（额外信息，重点）
- **`Using index`**：使用了**覆盖索引**，`SELECT` 的字段都能从索引树直接获取，无需回表。
- **`Using where`**：用 `WHERE` 处理结果，且查询列未被索引覆盖。
- **`Using index condition`**：列不完全被索引覆盖，`WHERE` 中是前导列的范围（索引下推 ICP）。
- **`Using temporary`**：需要创建临时表处理（如未索引字段 `DISTINCT`），通常需要优化。
- **`Using filesort`**：外部排序（内存或磁盘），一般需要加索引优化。
- **`Select tables optimized away`**：用聚合函数（`MAX`/`MIN`）访问索引字段，优化阶段已计算。

## 四、索引最佳实践（11 条）

基于示例表 `employees(id, name, age, position, hire_time)`，联合索引 `idx_name_age_position(name, age, position)`：

1. **全值匹配**：`WHERE name= ? AND age= ? AND position= ?` 命中三列。
2. **最左前缀法则**：查询从索引最左前列开始且**不跳过中间列**。跳过后缀列（如只用 `age`、`position`）会失效。
3. **索引列上不做操作**：计算、函数、自动/手动类型转换都会导致索引失效转向全表扫描。例：`LEFT(name,3)='LiLei'` 失效；`DATE(hire_time)='2018-09-30'` 失效，应改成日期范围查询。
4. **范围条件右边的列失效**：`name='LiLei' AND age>22 AND position='manager'` 中，`age` 之后的 `position` 无法用到索引。
5. **尽量用覆盖索引**：只访问索引包含的列，减少 `SELECT *`。
6. **不等于（`!=`/`<>`）、`NOT IN`、`NOT EXISTS`** 一般无法用索引，会全表扫描（`<`、`>`、`=` 优化器会综合评估）。
7. **`IS NULL` / `IS NOT NULL`** 一般也无法使用索引。
8. **`LIKE` 以通配符开头（`'%abc'`）索引失效**，变成全表扫描；`LIKE 'abc%'` 可用。解决 `%xx%`：用覆盖索引，或借助搜索引擎。
9. **字符串不加单引号索引失效**：`WHERE name=1000`（数字）失效，`WHERE name='1000'`（字符串）才生效。
10. **少用 `OR` / `IN`**：优化器会按检索比例、表大小等综合评估是否用索引，不一定走索引。
11. **范围查询优化**：单次范围过大时优化器可能放弃索引。可将大范围拆成多个小范围（如 `age>=1 AND age<=1000` 与 `age>=1001 AND age<=2000` 分开）。

## 五、一张图总结索引使用

- `LIKE 'KK%'` ≈ 等值常量，可用索引；
- `LIKE '%KK'` 与 `LIKE '%KK%'` ≈ 范围，索引失效（除非覆盖索引）。

> 核心心法：**查询从最左列起、别在列上做运算、范围后的列失效、尽量覆盖索引、少用 `OR`/`!=`/`%前导通配`**。
