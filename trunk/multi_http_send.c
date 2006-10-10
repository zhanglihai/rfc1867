/**********************************
 @author:zhanglihai
 @home  :www.zhanglihai.com
 @date  :2006/09/13
**********************************/
#include "multi_http_send.h"
#include <stdio.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <math.h>
#include <time.h>
#include <stdlib.h>
#include <fcntl.h>
#include <string.h>
int  http_multi_send(struct st_multi_req__ *req)
{
   struct st_multi_res__ res__;
   struct st_kv__ *ck_p; 
   int port =80;
   int url_len = strlen(req->url);
   int sockfd,frwp,frwp_tmp;
   int loop_p;
    unsigned int bytes,block_len=1024;
  unsigned long loc_p=0;
   char url[url_len];
   char *tmp_p;
   char host_port[url_len];
   char host[url_len];
   char *un_line="-----------------------------";
   char bound_id[200];
   char *header_str="POST %s HTTP/1.0\r\nUser-Agent:Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)\r\nContent-Type:multipart/form-data; boundary=%s\r\nContent-Length:%d\r\n\r\n";
  char  *tmp_file_str="tmp_multi_send.out";
  char  http_header[1024]; 
  struct sockaddr_in serv_addr;
  struct hostent *server;
  struct protoent *protocol;
  char buffer[1024];
  FILE *tmp_fp;
  FILE *loc_fp;
   sprintf(bound_id,"%s%d",un_line,time(0));
  //http:// len =7
   strcpy(url,req->url+7);
    //find first '/'  file.
   tmp_p=strchr(url,'/');
   if(tmp_p)
    {//file !='/'
       bzero(host_port,url_len);
       strncpy(host_port,url,strlen(url)-strlen(tmp_p));
    }else{
         strcpy(host_port,tmp_p);
     }
 //find port
   tmp_p=strchr(host_port,':');
   if(tmp_p)
   {
      bzero(host,url_len);
      strncpy(host,host_port,strlen(host_port)-strlen(tmp_p));
      port=atoi(tmp_p+1);
   }else{
      strcpy(host,host_port);
   }
   
  //open connection...
   protocol = getprotobyname(PROTOCOL);
   sockfd = socket(AF_INET,SOCK_STREAM,protocol->p_proto);
   if(sockfd<0)
   {
      printf("sockfd error."); 
      return -1;
   }
   server = gethostbyname(host);
   if(server==NULL)
   {
       printf("server == null");
      return -1;
   }
      bzero((char *)&serv_addr,sizeof(serv_addr));
      serv_addr.sin_family=AF_INET;
      serv_addr.sin_port=htons(port);
   // serv_addr.sin_addr=*((struct in_addr *)server->h_addr);
     bcopy((char *)server->h_addr,(char *)&serv_addr.sin_addr.s_addr,server->h_length);
     if(connect(sockfd,(struct sockaddr *)&serv_addr,sizeof(serv_addr))<0)
     {
         printf("ERROR connecting ");
        return -1;
     }

    tmp_fp=fopen(tmp_file_str,"wb");
    if(tmp_fp==NULL)
    {
         printf("tmp_file can't open");
        return -1;
    }
    for(ck_p=req->text_fields;ck_p<req->text_fields+req->text_field_count;ck_p++)
    {
       fprintf(tmp_fp,"%s\r\n",bound_id);
       fprintf(tmp_fp,"Content-Disposition: form-data; name=\"%s\"\r\n",ck_p->name);
       fprintf(tmp_fp,"\r\n");
       fprintf(tmp_fp,"%s\r\n",ck_p->value);
    }

    for(loop_p=0,ck_p=req->files;ck_p<req->files+req->file_count;ck_p++,loop_p++)
    {
       loc_fp=fopen(ck_p->name,"r");
         if(loc_fp==NULL)continue;
       fprintf(tmp_fp,"%s\r\n",bound_id);
       fprintf(tmp_fp,"Content-Disposition: form-data; name=\"file_%d\";filename=\"%s\"\r\n",loop_p,ck_p->name);
       fprintf(tmp_fp,"\r\n");
          
        while(block_len)
        {
          if(fread(buffer,block_len,1,loc_fp))
          {
             fwrite(buffer,block_len,1,tmp_fp);
             loc_p=loc_p+block_len;
          }else{
                fseek(loc_fp,loc_p,SEEK_SET);
                block_len=block_len/2;
           }
	}
     fprintf(tmp_fp,"\r\n");
     fclose(loc_fp);
     loc_p=0;   
    }
 
	//add submit button....
	fprintf(tmp_fp,"%s\r\n",bound_id);
	fprintf(tmp_fp,"Content-Disposition: form-data; name=\"submit\"\r\n");
	fprintf(tmp_fp,"\r\n");
	fprintf(tmp_fp,"Submit\r\n");
	fprintf(tmp_fp,"%s--\r\n\r\n",bound_id);
       
        fseek(tmp_fp,0,SEEK_END);
	sprintf(http_header,header_str,req->url,bound_id+2,ftell(tmp_fp));
        fclose(tmp_fp);
	frwp = write(sockfd,http_header,strlen(http_header));
	frwp_tmp = open(tmp_file_str,O_RDONLY);
	if(frwp_tmp==-1){
	   return -1;
	}
	while((bytes=read(frwp_tmp,buffer,sizeof(buffer)))>0)
	{
	  write(sockfd,buffer,bytes);
	}
	 while((loop_p=read(sockfd,buffer,sizeof(buffer)-1)))
    	{
          write(1,buffer,loop_p);
    	}
	close(sockfd);
	close(frwp_tmp);
	printf("header-->%s\r\n",http_header);
        printf("host is %s,port:%d,url : %s\r\n",host,port,req->url);
        req->response=&res__;
   rmdir(tmp_file_str); 
 return 1;
}
void init_st_multi_req(struct st_multi_req__ *req)
{
   req->cookie_count=0;
   req->text_field_count=0;
   req->file_count=0;
}


