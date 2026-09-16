## elastic-search

[入门](https://www.ruanyifeng.com/blog/2017/08/elasticsearch.html)

### 查询

```shell
# 当前节点的所有 Index
curl -X GET 'http://localhost:9200/_cat/indices?v'

# 新建一个名叫weather的 Index
curl -X PUT 'localhost:9200/weather'

# 删除一个名叫weather的 Index
curl -X DELETE 'localhost:9200/weather'


curl -X PUT 'localhost:9200/test' -H 'Content-Type: application/json' -d '

{
  "settings":{
    "number_of_shards":3,
    "number_of_replicas":2
  },
  "mappings":{
    "properties":{
      "id":{"type":"long"},
      "name":{"type":"text","analyzer":"ik_smart"},
      "text":{"type":"text","analyzer":"ik_max_word"}
    }
  }
}
'

```
