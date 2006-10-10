/*****************************
  @author:zhanglihai
  @home:www.zhanglihai.com
  @date:2006/09/13
 *****************************/
#define PROTOCOL "tcp" 
struct st_kv__{
  char *name;
  char *value;
};
 struct st_multi_req__{
  int cookie_count, text_field_count, file_count;
  char *url;
  struct  st_kv__  *cookies;
  struct  st_kv__  *text_fields;
  struct  st_kv__  *files;
  struct  st_multi_res__ *response;
};
 struct st_multi_res__{
 int status_code,http_statuc_code;
 char *text;
 struct st_kv__ *headers;
};
void init_st_multi_req(struct st_multi_req__ *req);
int   http_multi_send(struct st_multi_req__ *req);
