package com.zhanglihai.util;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * <P>
 * Title:
 * </P>
 * <P>
 * Description:
 * </P>
 * <P>
 * Copyright: Copyright(c) 2006
 * </P>
 * <P>
 * Company:SOHU
 * </P>
 * 
 * @project : blog.sohu.com
 * @author : zhanglihai
 * @home : http://www.zhanglihai.com
 * @email : zhanglihai.com@gmail.com
 * @date : 2006-9-4
 * @file : MultiPartDataSender.java
 * @version : 1.0
 */
/**
 * 本程序是客户端程序或者后台程序模拟浏览器的upload对数据进行装箱操作 . 具体协议请参考w3c RFC1867规范 有任何意见请到这里与我交流:
 * 
 * http://www.zhanglihai.com/blog/c_359_Socket_Upload.html
 */

public class MultiPartDataSender {

	private String host = "";

	private String url = "";

	private int port = 80;

	private Socket s = null;

	private BufferedReader sin;

	private BufferedOutputStream sout;

	private String responseCharset = "UTF-8";

	private String requestCharset = "UTF-8";

	private ArrayList<String> localFiles = new ArrayList<String>(3);

	private HashMap<String, String> textFields = new HashMap<String, String>(5);

	private static Logger log = Logger.getLogger(MultiPartDataSender.class);

	private HashMap<String, String> cookies = new HashMap<String, String>(3);

	private HashMap<String, String> headers = new HashMap<String, String>(3);

	private int statusCode = -1;

	private String uri;

	private String responseText = null;

	public void setUrl(String url) {
		this.url = url;
		try {
			URL url_ = new URL(url);
			this.host = url_.getHost();
			this.port = url_.getPort();
			this.port = port == -1 ? 80 : port;
			this.uri = url_.getFile();
		} catch (Exception e) {
			log.error("", e);
		}
	}

	public boolean connect() {
		try {
			s = new Socket(host, port);
			this.sin = new BufferedReader(new InputStreamReader(s
					.getInputStream(), responseCharset));
			this.sout = new BufferedOutputStream(s.getOutputStream());
			return true;
		} catch (Exception ex) {
			log.error("", ex);
			return false;
		}
	}

	public boolean send() {
		try {
			byte[] data = new byte[1024];
			int len = -1;

			String boundId = "-----------------------------"
					+ System.currentTimeMillis() + "ab20d";
			File tmpFile = new File(System.getProperties().getProperty(
					"java.io.tmpdir", "/opt/"), "upload_tmp_"
					+ System.currentTimeMillis());
			FileOutputStream tmpFout = new FileOutputStream(tmpFile);
			StringBuffer content = new StringBuffer();
			String name;
			for (Iterator<String> it = this.textFields.keySet().iterator(); it
					.hasNext();) {
				name = it.next();
				content.append(boundId).append("\r\n");
				content.append("Content-Disposition: form-data; name=\"")
						.append(name).append("\"\r\n");
				content.append("\r\n");
				content.append(
						new String(textFields.get(name)
								.getBytes(requestCharset))).append("\r\n");
			}
			tmpFout.write(content.toString().getBytes());
			tmpFout.flush();

			int p = 0;
			for (String file : localFiles) {
				content.setLength(0);
				content.append(boundId).append("\r\n");
				content.append("Content-Disposition: form-data; name=\"file_")
						.append(p++).append("\"; filename=\"").append(file)
						.append("\"\r\n");
				content.append("Content-Type: ").append(getContentType(file))
						.append("\r\n");
				content.append("\r\n");
				tmpFout.write(content.toString().getBytes());
				tmpFout.flush();
				FileInputStream fin = null;
				try {
					fin = new FileInputStream(file);

					while ((len = fin.read(data, 0, data.length)) != -1) {
						tmpFout.write(data, 0, len);
					}
					tmpFout.flush();
					tmpFout.write("\r\n".getBytes());
				} catch (Exception ex) {
					log.error("read file .." + file, ex);
				} finally {
					try {
						fin.close();
					} catch (Exception ex) {
					}
				}

			}//
			content.setLength(0);
			content.append(boundId + "\r\n");
			content
					.append("Content-Disposition: form-data; name=\"submit\"\r\n");
			content.append("\r\n");
			content.append("Submit\r\n");
			content.append(boundId).append("--");
			tmpFout.write(content.toString().getBytes());
			tmpFout.flush();
			tmpFout.close();
			content.setLength(0);
			StringBuffer header = new StringBuffer();
			header.append("POST ").append(uri).append(" HTTP/1.1\r\n");
			header.append("Host:").append(host).append("\r\n");
			header
					.append("User-Agent:Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)\r\n");
			header
					.append("Accept: text/html, application/xml;q=0.9, application/xhtml+xml, image/png, image/jpeg, image/gif, image/x-xbitmap, */*;q=0.1\r\n");
			header
					.append("Accept-Charset: gbk, utf-8, utf-16, iso-8859-1;q=0.6, *;q=0.1\r\n");
			header.append("Connection:close\r\n");
			if (headers.size() > 0) {
				for (Iterator<String> it = headers.keySet().iterator(); it
						.hasNext();) {
					name = it.next();
					header.append(name).append(":").append(headers.get(name))
							.append("\r\n");
				}
			}

			header.append("Content-Length:" + (tmpFile.length()) + "\r\n");
			header.append("Content-Type:multipart/form-data; boundary="
					+ boundId.substring(2) + "\r\n");
			p = 1;
			if (cookies.size() > 0) {
				header.append("Cookie:");
				for (Iterator<String> it = cookies.keySet().iterator(); it
						.hasNext();) {
					name = it.next();
					header.append(name).append("=").append(cookies.get(name));
					if (p++ != cookies.size())
						header.append("; ");
				}
			}
			header.append("\r\n\r\n");
			sout.write(header.toString().getBytes());
			FileInputStream tmpFin = new FileInputStream(tmpFile);
			while ((len = tmpFin.read(data, 0, data.length)) != -1) {
				sout.write(data, 0, len);
			}
			sout.flush();
			tmpFin.close();
			String line;
			line = sin.readLine();
			statusCode = Integer.parseInt(line.substring(9, 12));
			content.setLength(0);
			boolean isHeader = true;
			while ((line = sin.readLine()) != null) {
				if (line.trim().equals("") && isHeader) {
					isHeader = false;
				}
				if (!isHeader) {
					content.append(line).append("\r\n");
				}
				if (isHeader)
					System.out.println(line);
			}
			tmpFile.delete();
			this.responseText = content.toString();
			return this.statusCode == 200;
		} catch (Exception e) {
			log.error("", e);
			return false;
		}
	}

	public void close() {
		this.cookies.clear();
		this.localFiles.clear();
		this.textFields.clear();
		this.headers.clear();
		try {
			sin.close();
		} catch (Exception e) {
		}
		try {
			sout.close();
		} catch (Exception e) {
		}
		try {
			s.close();
		} catch (Exception e) {
		}

	}

	public void addTextField(String name, String value) {
		if (name != null && value != null) {
			this.textFields.put(name, value);
		}
	}

	public void addCookie(String name, String value) {
		this.cookies.put(name, value);
	}

	public void addLocalFile(String file) {
		this.localFiles.add(file);
	}

	public void addHeaders(String name, String value) {
		if (name == null)
			return;
		String tmpName = name.toLowerCase().trim();
		//内置7个参数
		if (!tmpName.equals("user-agent")
				&& !tmpName.equals("host")
				&& !tmpName.equals("accept-charset")
				&& !tmpName.equals("connection")
				&& !tmpName.equals("content-type")
				&& !tmpName.equals("content-length")
				&& !tmpName.equals("accept"))
			this.headers.put(name, value);
	}

	public MultiPartDataSender() {
	}

	public void setRequestCharset(String charset) {
		this.requestCharset = charset;
	}

	public void setResponseCharset(String charset) {
		this.responseCharset = charset;
	}

	public int getStatusCode() {
		return this.statusCode;
	}

	public String getResponseText() {
		return this.responseText;
	}

	private static String getContentType(String file) {
		try {
			URL furl = new URL("file:/" + file);
			String mimeType = furl.openConnection().getContentType();
			if (mimeType == null || mimeType.indexOf("unknow") != -1)
				return "application/octet-stream";
			return mimeType;
		} catch (Exception e) {
			return "application/octet-stream";
		}
	}

}

/*******************************************************************************
 * Apache License Version 2.0, January 2004 http://www.apache.org/licenses/
 * 
 * TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION
 * 
 * 1. Definitions.
 * 
 * "License" shall mean the terms and conditions for use, reproduction, and
 * distribution as defined by Sections 1 through 9 of this document.
 * 
 * "Licensor" shall mean the copyright owner or entity authorized by the
 * copyright owner that is granting the License.
 * 
 * "Legal Entity" shall mean the union of the acting entity and all other
 * entities that control, are controlled by, or are under common control with
 * that entity. For the purposes of this definition, "control" means (i) the
 * power, direct or indirect, to cause the direction or management of such
 * entity, whether by contract or otherwise, or (ii) ownership of fifty percent
 * (50%) or more of the outstanding shares, or (iii) beneficial ownership of
 * such entity.
 * 
 * "You" (or "Your") shall mean an individual or Legal Entity exercising
 * permissions granted by this License.
 * 
 * "Source" form shall mean the preferred form for making modifications,
 * including but not limited to software source code, documentation source, and
 * configuration files.
 * 
 * "Object" form shall mean any form resulting from mechanical transformation or
 * translation of a Source form, including but not limited to compiled object
 * code, generated documentation, and conversions to other media types.
 * 
 * "Work" shall mean the work of authorship, whether in Source or Object form,
 * made available under the License, as indicated by a copyright notice that is
 * included in or attached to the work (an example is provided in the Appendix
 * below).
 * 
 * "Derivative Works" shall mean any work, whether in Source or Object form,
 * that is based on (or derived from) the Work and for which the editorial
 * revisions, annotations, elaborations, or other modifications represent, as a
 * whole, an original work of authorship. For the purposes of this License,
 * Derivative Works shall not include works that remain separable from, or
 * merely link (or bind by name) to the interfaces of, the Work and Derivative
 * Works thereof.
 * 
 * "Contribution" shall mean any work of authorship, including the original
 * version of the Work and any modifications or additions to that Work or
 * Derivative Works thereof, that is intentionally submitted to Licensor for
 * inclusion in the Work by the copyright owner or by an individual or Legal
 * Entity authorized to submit on behalf of the copyright owner. For the
 * purposes of this definition, "submitted" means any form of electronic,
 * verbal, or written communication sent to the Licensor or its representatives,
 * including but not limited to communication on electronic mailing lists,
 * source code control systems, and issue tracking systems that are managed by,
 * or on behalf of, the Licensor for the purpose of discussing and improving the
 * Work, but excluding communication that is conspicuously marked or otherwise
 * designated in writing by the copyright owner as "Not a Contribution."
 * 
 * "Contributor" shall mean Licensor and any individual or Legal Entity on
 * behalf of whom a Contribution has been received by Licensor and subsequently
 * incorporated within the Work.
 * 
 * 2. Grant of Copyright License. Subject to the terms and conditions of this
 * License, each Contributor hereby grants to You a perpetual, worldwide,
 * non-exclusive, no-charge, royalty-free, irrevocable copyright license to
 * reproduce, prepare Derivative Works of, publicly display, publicly perform,
 * sublicense, and distribute the Work and such Derivative Works in Source or
 * Object form.
 * 
 * 3. Grant of Patent License. Subject to the terms and conditions of this
 * License, each Contributor hereby grants to You a perpetual, worldwide,
 * non-exclusive, no-charge, royalty-free, irrevocable (except as stated in this
 * section) patent license to make, have made, use, offer to sell, sell, import,
 * and otherwise transfer the Work, where such license applies only to those
 * patent claims licensable by such Contributor that are necessarily infringed
 * by their Contribution(s) alone or by combination of their Contribution(s)
 * with the Work to which such Contribution(s) was submitted. If You institute
 * patent litigation against any entity (including a cross-claim or counterclaim
 * in a lawsuit) alleging that the Work or a Contribution incorporated within
 * the Work constitutes direct or contributory patent infringement, then any
 * patent licenses granted to You under this License for that Work shall
 * terminate as of the date such litigation is filed.
 * 
 * 4. Redistribution. You may reproduce and distribute copies of the Work or
 * Derivative Works thereof in any medium, with or without modifications, and in
 * Source or Object form, provided that You meet the following conditions:
 * 
 * (a) You must give any other recipients of the Work or Derivative Works a copy
 * of this License; and
 * 
 * (b) You must cause any modified files to carry prominent notices stating that
 * You changed the files; and
 * 
 * (c) You must retain, in the Source form of any Derivative Works that You
 * distribute, all copyright, patent, trademark, and attribution notices from
 * the Source form of the Work, excluding those notices that do not pertain to
 * any part of the Derivative Works; and
 * 
 * (d) If the Work includes a "NOTICE" text file as part of its distribution,
 * then any Derivative Works that You distribute must include a readable copy of
 * the attribution notices contained within such NOTICE file, excluding those
 * notices that do not pertain to any part of the Derivative Works, in at least
 * one of the following places: within a NOTICE text file distributed as part of
 * the Derivative Works; within the Source form or documentation, if provided
 * along with the Derivative Works; or, within a display generated by the
 * Derivative Works, if and wherever such third-party notices normally appear.
 * The contents of the NOTICE file are for informational purposes only and do
 * not modify the License. You may add Your own attribution notices within
 * Derivative Works that You distribute, alongside or as an addendum to the
 * NOTICE text from the Work, provided that such additional attribution notices
 * cannot be construed as modifying the License.
 * 
 * You may add Your own copyright statement to Your modifications and may
 * provide additional or different license terms and conditions for use,
 * reproduction, or distribution of Your modifications, or for any such
 * Derivative Works as a whole, provided Your use, reproduction, and
 * distribution of the Work otherwise complies with the conditions stated in
 * this License.
 * 
 * 5. Submission of Contributions. Unless You explicitly state otherwise, any
 * Contribution intentionally submitted for inclusion in the Work by You to the
 * Licensor shall be under the terms and conditions of this License, without any
 * additional terms or conditions. Notwithstanding the above, nothing herein
 * shall supersede or modify the terms of any separate license agreement you may
 * have executed with Licensor regarding such Contributions.
 * 
 * 6. Trademarks. This License does not grant permission to use the trade names,
 * trademarks, service marks, or product names of the Licensor, except as
 * required for reasonable and customary use in describing the origin of the
 * Work and reproducing the content of the NOTICE file.
 * 
 * 7. Disclaimer of Warranty. Unless required by applicable law or agreed to in
 * writing, Licensor provides the Work (and each Contributor provides its
 * Contributions) on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied, including, without limitation, any
 * warranties or conditions of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or
 * FITNESS FOR A PARTICULAR PURPOSE. You are solely responsible for determining
 * the appropriateness of using or redistributing the Work and assume any risks
 * associated with Your exercise of permissions under this License.
 * 
 * 8. Limitation of Liability. In no event and under no legal theory, whether in
 * tort (including negligence), contract, or otherwise, unless required by
 * applicable law (such as deliberate and grossly negligent acts) or agreed to
 * in writing, shall any Contributor be liable to You for damages, including any
 * direct, indirect, special, incidental, or consequential damages of any
 * character arising as a result of this License or out of the use or inability
 * to use the Work (including but not limited to damages for loss of goodwill,
 * work stoppage, computer failure or malfunction, or any and all other
 * commercial damages or losses), even if such Contributor has been advised of
 * the possibility of such damages.
 * 
 * 9. Accepting Warranty or Additional Liability. While redistributing the Work
 * or Derivative Works thereof, You may choose to offer, and charge a fee for,
 * acceptance of support, warranty, indemnity, or other liability obligations
 * and/or rights consistent with this License. However, in accepting such
 * obligations, You may act only on Your own behalf and on Your sole
 * responsibility, not on behalf of any other Contributor, and only if You agree
 * to indemnify, defend, and hold each Contributor harmless for any liability
 * incurred by, or claims asserted against, such Contributor by reason of your
 * accepting any such warranty or additional liability.
 * 
 * END OF TERMS AND CONDITIONS
 * 
 * APPENDIX: How to apply the Apache License to your work.
 * 
 * To apply the Apache License to your work, attach the following boilerplate
 * notice, with the fields enclosed by brackets "[]" replaced with your own
 * identifying information. (Don't include the brackets!) The text should be
 * enclosed in the appropriate comment syntax for the file format. We also
 * recommend that a file or class name and description of purpose be included on
 * the same "printed page" as the copyright notice for easier identification
 * within third-party archives.
 * 
 * Copyright [yyyy] [name of copyright owner]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
