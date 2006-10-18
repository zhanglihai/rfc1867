using System;
using System.Collections.Generic;
using System.Text;
using System.Collections;
using System.IO;
using System.Net;
using System.Net.Sockets;

/****************************************************************
 * 客户端HTTP上传工具
 * 
 * @author  :   zhanglihai
 * @email   :   zhanglihai.com@gmail.com
 * @home    :   http://www.zhanglihai.com
 * @date    :   2006/10/12
 * @version :   1.0
 * 
 * 
 * @project :   http://code.google.com/p/rfc1867/
 *              
 *              Other Linux C,Java Edtion.
 * 
 * *************************************************************/
namespace multi_http_send
{
    public class MultiPartDataSender
    {

        private ArrayList localFiles = new ArrayList(5);
        private Hashtable cookies = new Hashtable(5);
        private Hashtable textFields = new Hashtable(8);
        private Hashtable responseHeaders = new Hashtable(8);
        private Hashtable requestHeaders = new Hashtable(8);
        private string uploadUrl;
        private string requestCharset = "UTF-8";
        private string responseCharset = "UTF-8";
        private int statusCode = -1;
        private string responseText;
        private string tmpUploadFile;
        private TcpClient client;
        private StreamReader sin;
        private NetworkStream sout;
        private string host;
        private string file;
        private int port = 80;
        public MultiPartDataSender()
        {
            this.tmpUploadFile = Path.GetTempFileName();
        }
        /********************************************/
        /*设置远程HTTP URL
        /****************************************/
        public string RemoteURL
        {
            set
            {
                this.uploadUrl = value;    
            }
        }


        public string RequestCharset
        {

            set
            {
                this.requestCharset = value;
            }
        }

        public string ResponseCharset
        {

            set
            {
                this.responseCharset = value;
            }
        }


        public int StatusCode
        {

            get
            {
                return this.statusCode;
            }

        }

       //获得返回正文文本内容
        //
        public string ResponseText
        {

            get
            {
                return this.responseText;
            }
        }

        public void AddLocalFile(string file)
        {
            if(file!=null&&file.Trim().Length>0)
             this.localFiles.Add(file);
        }

        public void AddTextField(string name, string value)
        {
            if (name != null && value != null && name.Trim().Length > 0 && value.Trim().Length > 0)
                    this.textFields.Add(name, value);
        }


        public void AddHeader(string name, string value)
        {
            		if (name == null)
			return;
		if(value==null)return;
        String tmpName = name.ToLower().Trim();
		//内置7个参数
        if (!tmpName.Equals("user-agent")
                && !tmpName.Equals("host")
                && !tmpName.Equals("accept-charset")
                && !tmpName.Equals("connection")
                && !tmpName.Equals("content-type")
                && !tmpName.Equals("content-length")
                && !tmpName.Equals("accept"))
            requestHeaders.Add(name, value);

        }

        public void AddCookie(string name, string value)
        {

            if(name!=null&&value!=null&&name.Trim().Length>0&&value.Trim().Length>0)
                    this.cookies.Add(name, value);
        }

        public bool Connect()
        {
            try
            {
                Uri uri = new Uri(uploadUrl);
                this.host = uri.Host;
                this.file = uri.PathAndQuery;
                this.port = uri.Port;
                client = new TcpClient(host, port);
                this.sin = new StreamReader(client.GetStream());
                this.sout = client.GetStream();
                return true;

            }
            catch (Exception)
            {
                return false;
            }
           
        }


        public bool Send()
        {
            string boundId = "-----------------------------" + DateTime.Now.ToString().Replace(' ', '0').Replace(':', '0').Replace('-','0');

            try
            {
                byte[] data;
                FileStream fin = new FileStream(tmpUploadFile, FileMode.OpenOrCreate);
                StringBuilder buf = new StringBuilder();
                for (IDictionaryEnumerator de = textFields.GetEnumerator(); de.MoveNext(); )
                {
                    buf.Append(boundId).Append("\r\n");
                    buf.Append("Content-Disposition: form-data; name=\"").Append(de.Key).Append("\"\r\n\r\n");
                    buf.Append(de.Value).Append("\r\n");
                }
                data = System.Text.Encoding.GetEncoding(requestCharset).GetBytes(buf.ToString());
                fin.Write(data,0,data.Length);
               
                buf.Length = 0;
                byte[] buffer = new byte[1024];
                int len = -1;
                
                for (int f = 0; f < localFiles.Count; f++)
                {

                    try
                    {
                       
                        FileStream attFile = new FileStream(localFiles[f] as string,FileMode.Open);
                        string attFilename;
                        attFilename = localFiles[f] as string;
                        attFilename = attFilename.Replace(@"\","/");

                        int index_p = attFilename.LastIndexOf("/");
                        if (index_p != -1)
                            attFilename = attFilename.Substring(index_p + 1);
                        buf.Append(boundId).Append("\r\n");
                        buf.Append("Content-Disposition: form-data; name=\"file_").Append(f).Append("\";filename=\"D:/a/").Append(attFilename).Append("\"\r\n");
                        buf.Append("Content-Type:application/octet-stream\r\n");
                        buf.Append("\r\n");
                        data = System.Text.Encoding.ASCII.GetBytes(buf.ToString());
                        fin.Write(data, 0, data.Length);
                        fin.Flush();
                        while ((len = attFile.Read(buffer, 0, buffer.Length)) >0)
                        {
                            fin.Write(buffer, 0, len);
                        }
                        fin.Flush();
                        fin.Write(System.Text.Encoding.ASCII.GetBytes("\r\n"), 0, 2);
                        attFile.Close();
                        buf.Length = 0;
                       
                    }
                    catch (Exception)
                    {
                        continue;
                    }


                }
                buf.Length = 0;

                /////
                buf.Append(boundId).Append("\r\n");
                buf.Append("Content-Disposition: form-data; name=\"submit\"\r\n\r\n");
                buf.Append("Submit\r\n");
                buf.Append(boundId).Append("--");
                data = System.Text.Encoding.ASCII.GetBytes(buf.ToString());
                fin.Write(data, 0, data.Length);
                fin.Flush();


  

                buf.Length = 0;
                buf.Append("POST ").Append(file).Append(" HTTP/1.0\r\n");
                buf.Append("Content-Type:multipart/form-data; boundary=").Append(boundId.Substring(2)).Append("\r\n");
                buf.Append("Connection:close\r\n");
                buf.Append("User-Agent:Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)\r\n");
                buf.Append("Host:").Append(host);
                if (port != 80)
                    buf.Append(":").Append(port);
                buf.Append("\r\n"); 
                buf.Append("Accept-Language:zh-cn,zh;q=0.5\r\n");
                buf.Append("Accept-Charset:gbk,utf-8,utf-16,iso-8859-1;q=0.6, *;q=0.1\r\n");
                buf.Append("Accept:text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5\r\n");
                buf.Append("Content-Length:").Append(fin.Length).Append("\r\n");

                for (IDictionaryEnumerator de = requestHeaders.GetEnumerator(); de.MoveNext(); )
                {
                    buf.Append(de.Key).Append(":").Append(de.Value).Append("\r\n");
                }

                len = 0;
                for (IDictionaryEnumerator de = cookies.GetEnumerator(); de.MoveNext(); )
                {
                    if(len++==0)
                        buf.Append("Cookie:");
                    buf.Append(de.Key).Append("=").Append(de.Value);
                    if (len != cookies.Count)
                        buf.Append(";");
                    
                }
                buf.Append("\r\n\r\n");

                data = System.Text.Encoding.GetEncoding(requestCharset).GetBytes(buf.ToString());
                sout.Write(data, 0, data.Length);
                fin.Seek(0, SeekOrigin.Begin);
                while ((len = fin.Read(buffer, 0, buf.Length)) > 0)
                {

                    sout.Write(buffer, 0, len);
                }
                sout.Flush();
                fin.Close();
                buf.Length = 0;
                string line = sin.ReadLine();
  
                this.statusCode = Convert.ToInt32(line.Substring(9, 3));
                bool isHeader = true;
                string[] kv;
                while ((line = sin.ReadLine()) != null)
                {
                    if (isHeader)
                    {
                        kv = line.Split(':');
                        if (kv.Length == 2)
                            responseHeaders.Add(kv[0].Trim().ToLower(), kv[1]);
                        if (line.Trim().Equals(""))
                            isHeader = false;
                    }
                    else
                    {
                        buf.Append(line).Append("\n");
                    }

                }
                this.responseText = buf.ToString();
                return this.statusCode==200;
            }
            catch (Exception e)
            {
                return false;
            }
        }

        public void Close()
        {

            this.responseHeaders.Clear();
            this.textFields.Clear();
            this.cookies.Clear();
            this.localFiles.Clear();
            try
            {
                File.Delete(this.tmpUploadFile);
            }
            catch { }
            
            try
            {
               sout.Close();
            }
            catch { }

            try
            {
                this.sin.Close();
            }catch{}

            try
            {
                this.client.Close();
            }
            catch { }

        }

        public  ICollection  GetHeaderNames()
        {
            return responseHeaders.Keys;
        }

        public string GetHeader(string name)
        {
            return responseHeaders[name.ToLower()] as string;
        }

    }
}
