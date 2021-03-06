
import com.itextpdf.text.pdf.BaseFont;
import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.StringUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @Author: HeTao
 * @DateTime: 2021/10/29
 * @Description: pdf 导出
 */

@Slf4j
@Component
public class PdfExportHandler {
    private static PdfExportHandler pdfExportHandler;
    @Autowired
    FreeMarkerConfigurer freeMarkerConfigurer;
    private static final String PDF_TYPE = "application/pdf";
    private static final String DEFAULT_ENCODING = "utf-8";
    private static final boolean DEFAULT_NOCACHE = true;
    private static final CharSequence HEADER_ENCODING = "utf-8";
    private static final CharSequence HEADER_NOCACHE = "no-cache";

    @PostConstruct
    public void init(){
            pdfExportHandler = this;
            pdfExportHandler.freeMarkerConfigurer = this.freeMarkerConfigurer;
    }

    /**
     * 生成PDF文件流
     * @param ftlName freemarker模板文件名称
     * @param exportService  查询接口
     * @param exportClass  查询返回数据实体类Class
     * @param query  查询条件
     * @return ByteArrayOutputStream
     * @throws Exception 异常
     */
    public static<Q, R> ByteArrayOutputStream createPDF(PdfExportFunction<Q,R> exportService,Class<?> exportClass,Q query, String ftlName) throws Exception {
        Configuration cfg = new Configuration();
        try {
            cfg.setLocale(Locale.CHINA);
            cfg.setEncoding(Locale.CHINA, "UTF-8");
            //设置编码
            cfg.setDefaultEncoding("UTF-8");

            //获取模板
            Template template = pdfExportHandler.freeMarkerConfigurer.getConfiguration().getTemplate(ftlName);
            template.setEncoding("UTF-8");
            template.setLocale(Locale.CHINA);
            template.setOutputEncoding("UTF-8");
            ITextRenderer iTextRenderer = new ITextRenderer();
            //设置字体
            ITextFontResolver fontResolver = iTextRenderer.getFontResolver();
            fontResolver.addFont(ResourceUtils.getFile("templates/font/HarmonyOS Sans SC.ttf").getPath(), BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);

            Writer writer = new StringWriter();
            //数据填充模板
            //查询数据库中的数据
            int pageNum = 1;
            List<R> dataList=exportService.queryPdfExportList(query,pageNum);
            Map<String,Object> map = new HashMap<>();
            //模板遍历时的名称<#list dataList as item>
            map.put("dataList",dataList);
            template.process(map, writer);

            //设置输出文件内容及路径
            String str = writer.toString();
            iTextRenderer.setDocumentFromString(str);
            iTextRenderer.getSharedContext().setBaseURL("file:/home/");//共享路径 模板插入图片时会寻找该路径下的图片
            iTextRenderer.layout();

            //生成PDF
            ByteArrayOutputStream baos = new ByteArrayOutputStream();


            iTextRenderer.createPDF(baos);
            baos.close();
            String message = "生成PDF线程名称---->"+Thread.currentThread().getName();
            System.out.println(message);
            return baos;
        } catch(Exception e) {
            log.error(e.getMessage(),e);
            throw new Exception(e);
        }
    }

    /**
     *
     * @param response 响应
     * @param bytes 将返回的ByteArrayOutputStream用.toByteArray()转为Byte后传入
     * @param filename 生成的文件名称
     */
    public static void renderPdf(HttpServletResponse response, final byte[] bytes, final String filename) {
        initResponseHeader(response, PDF_TYPE);
        setFileDownloadHeader(response, filename, ".pdf");
        if (null != bytes) {
            try {
                response.getOutputStream().write(bytes);
                response.getOutputStream().flush();
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

    }


    /**
     * 分析并设置contentType与headers.
     */
    private static HttpServletResponse initResponseHeader(HttpServletResponse response, final String contentType, final String... headers) {
        // 分析headers参数
        String encoding = DEFAULT_ENCODING;
        boolean noCache = DEFAULT_NOCACHE;
        for (String header : headers) {
            String headerName = StringUtils.substringBefore(header, ":");
            String headerValue = StringUtils.substringAfter(header, ":");
            if (StringUtils.equalsIgnoreCase(headerName, HEADER_ENCODING)) {
                encoding = headerValue;
            } else if (StringUtils.equalsIgnoreCase(headerName, HEADER_NOCACHE)) {
                noCache = Boolean.parseBoolean(headerValue);
            } else {
                throw new IllegalArgumentException(headerName + "不是一个合法的header类型");
            }
        }
        // 设置headers参数
        String fullContentType = contentType + ";charset=" + encoding;
        response.setContentType(fullContentType);
        if (noCache) {
            // Http 1.0 header
            response.setDateHeader("Expires", 0);
            response.addHeader("Pragma", "no-cache");
            // Http 1.1 header
            response.setHeader("Cache-Control", "no-cache");
        }
        return response;
    }


    /**
     * 设置让浏览器弹出下载对话框的Header.
     */
    public static void setFileDownloadHeader(HttpServletResponse response, String fileName, String fileType) {
//        try {
        // 中文文件名支持
//            String encodedfileName = new String(fileName.getBytes("GBK"), "ISO8859-1");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + fileType + "\"");
//        } catch (UnsupportedEncodingException e) {
//            log.error(e.getMessage(), e);
//        }
    }
}
