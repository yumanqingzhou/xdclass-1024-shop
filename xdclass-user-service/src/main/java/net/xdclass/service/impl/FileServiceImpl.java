package net.xdclass.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSBuilder;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.config.OssConfig;
import net.xdclass.service.FileService;
import net.xdclass.utils.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class FileServiceImpl implements FileService {
    @Autowired
    private OssConfig ossConfig;

    /**
     * OSS上传用户头像
     * @param file
     * @return
     */
    @Override
    public String uploadUserImg(MultipartFile file) {
        //获取OSS客户端创建参数
        String bucketname = ossConfig.getBucketname();
        String accessKeyId = ossConfig.getAccessKeyId();
        String endpoint = ossConfig.getEndpoint();
        String accessKeySecret = ossConfig.getAccessKeySecret();
        //创建OSS客户端
        OSS ossCline = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        //获取原生文件名
        String originalFilename = file.getOriginalFilename();
        //截取原生文件名后缀
        String endStr = originalFilename.substring(originalFilename.lastIndexOf("."));
        //JDK8获取当前系统时间  创建文件夹
        LocalDateTime ldt = LocalDateTime.now();
        //格式化当前时间
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        String folder = dtf.format(ldt);
        //拼装路径  user/2022/12/1/sdfdsafsdfdsf.jpg
        String newFileName="user/"+folder+"/"+ CommonUtil.getUUID()+endStr;
        //上传
        try {
            PutObjectResult putObjectResult = ossCline.putObject(bucketname, newFileName, file.getInputStream());
            if (putObjectResult!=null){
                //拼接图片上传后地址字符串返回给前段
                String imgUrl = "https://"+bucketname+"."+endpoint+"/"+newFileName;
                return imgUrl;
            }
        } catch (IOException e) {
            log.error("文件上传失败:{}",e);
            e.printStackTrace();
        }finally {
            ossCline.shutdown();
        }
        return null;
    }
}
