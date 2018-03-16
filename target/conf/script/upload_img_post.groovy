import org.d.sps.DBGW.RETURN
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.regex.Pattern
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.sanselan.ImageInfo
import org.apache.sanselan.ImageReadException
import org.apache.sanselan.Sanselan
import org.d.sps.*

byte[] imgB1 = Base64.decodeBase64(img.getBytes());
ImageInfo imgI1 = Sanselan.getImageInfo(imgB1);
if (imgI1.getHeight()<200 || imgI1.getWidth()<300){
	return new RETURN("serviceType",serviceType,"trace_id",trace_id,"result","Kich thuoc anh tu 200 - 3000","tpl","invalid_req","body",body);
}
if (imgI1.getHeight()>2000 || imgI1.getWidth()>3000){
	return new RETURN("serviceType",serviceType,"trace_id",trace_id,"result","Kich thuoc anh tu 200 - 3000","tpl","invalid_req","body",body);
}

String strFolder = DBGW.fdate("MMyyyy/dd");
String f1 = ma_kh + "_" + id_anh.substring(0, id_anh.indexOf("|")) + "_" + DBGW.fdate("yyyyMMdd_HHmmss") + "." + ten_anh.substring(ten_anh.lastIndexOf('.')+1);

FTPClient ftpClient = new FTPClient();

ftpClient.connect("10.156.3.30",21);
ftpClient.login("ftp_vnp", "ftp\$123");

ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

boolean done1 = ftpClient.storeFile(strFolder+"/"+f1,new ByteArrayInputStream(imgB1));
if (!done1){
	return new RETURN("serviceType",serviceType,"trace_id",trace_id,"result","Upload khong thanh cong","tpl","exception","body",body);
}
ftpClient.logout();
ftpClient.disconnect();

String iu1=utility.val("begin :1 := ADMIN_V2.PKG_IMAGE.upload_image(?,?,?,?,?,?,?,?,?,?,?,?); end;",[ma_tb,ma_kh,ma_hd,loai_kh,loai_anh,id_anh,ten_anh,strFolder,ly_do,ngay_tao_anh,userid,ip],"ccbs2");
if ( !(iu1.startsWith("0")) ){
	return new RETURN("serviceType",serviceType,"trace_id",trace_id,"result","Cap nhat DB khong thanh cong","tpl","exception","body",body);
}

return new RETURN("serviceType",serviceType,"trace_id",trace_id,"result","Upload anh thanh cong","tpl","tpl_val","body",body);