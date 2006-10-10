#include <stdio.h>
#include "multi_http_send.h"
int main(){
 struct st_multi_req__ req;
 struct st_kv__ files[1];
 struct st_kv__ fields[2];
 init_st_multi_req(&req);
 req.text_field_count=2;
 fields[0].name="subject";
 fields[0].value="hello,";
 fields[1].name="content";
 fields[1].value="world";
 files[0].name="/home/zhanglihai/QQ.png";
 req.file_count=1;
 req.files=files;
 req.text_fields=fields;
 req.url="http://www.xxx.com/upload";// startWith 'http://'
 http_multi_send(&req);
}

