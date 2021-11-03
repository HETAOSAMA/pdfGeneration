

import java.util.List;

/**
 * @Author: HeTao
 * @DateTime: 2021/10/29
 * @Description: pdf 导出
 */
public interface PdfExportFunction<Q, R> {
    /**
     * 分页查询
     * @param queryVo 查询条件
     * @param pageNum 页码
     * @return 查询结果
     */
    public List<R> queryPdfExportList(Q queryVo, int pageNum);
}
