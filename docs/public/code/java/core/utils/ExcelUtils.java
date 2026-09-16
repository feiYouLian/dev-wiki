package cn.stylefeng.guns.sys.core.util;

import java.util.Date;

import cn.stylefeng.guns.sys.modular.system.entity.Product;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExcelUtils {

//    private static Logger LG = LoggerFactory.getLogger(ExcelUtils.class);


    /**
     * 利用JAVA的反射机制，将放置在JAVA集合中并且符号一定条件的数据以EXCEL 的形式输出到指定IO设备上<br>
     * 用于单个sheet
     *
     * @param <T>
     * @param headers 表格属性列名数组
     * @param dataset 需要显示的数据集合,集合中一定要放置符合javabean风格的类的对象。此方法支持的
     *                javabean属性的数据类型有基本数据类型及String,Date,String[],Double[]
     * @param out     与输出设备关联的流对象，可以将EXCEL文档导出到本地文件或者网络中
     */
    public static <T> void exportExcel(Map<String, String> headers, Collection<T> dataset, OutputStream out) throws Exception {
        exportExcel(headers, dataset, out, "yyyy-MM-dd");
    }

    /**
     * 利用JAVA的反射机制，将放置在JAVA集合中并且符号一定条件的数据以EXCEL 的形式输出到指定IO设备上<br>
     * 用于单个sheet
     *
     * @param <T>
     * @param headers 表格属性列名数组
     * @param dataset 需要显示的数据集合,集合中一定要放置符合javabean风格的类的对象。此方法支持的
     *                javabean属性的数据类型有基本数据类型及String,Date,String[],Double[]
     * @param out     与输出设备关联的流对象，可以将EXCEL文档导出到本地文件或者网络中
     * @param pattern 设定输出格式
     */
    public static <T> void exportExcel(Map<String, String> headers, Collection<T> dataset, OutputStream out, String pattern) throws Exception {
        // 声明一个工作薄
        Workbook workbook = new XSSFWorkbook();
        // 生成一个表格
        Sheet sheet = workbook.createSheet();

        ExcelUtils.write2Sheet(sheet, headers, dataset, pattern);
        workbook.write(out);

    }


    /**
     * 每个sheet的写入
     *
     * @param sheet   页签
     * @param headers 表头
     * @param dataset 数据集合
     * @param pattern 日期格式
     */
    private static <T> void write2Sheet(Sheet sheet, Map<String, String> headers, Collection<T> dataset, String pattern) throws Exception {
        // 产生表格标题行
        Row row = sheet.createRow(0);
        // 标题行转中文
        Set<String> keys = headers.keySet();
        Iterator<String> it1 = keys.iterator();
        int c = 0;   //标题列数
        while (it1.hasNext()) {
            String key = it1.next();
            String title = headers.get(key);
            Cell cell = row.createCell(c);
            XSSFRichTextString text = new XSSFRichTextString(title);
            cell.setCellValue(text);
            c++;
        }

        // 遍历集合数据，产生数据行
        Iterator<T> it = dataset.iterator();
        int index = 0;
        while (it.hasNext()) {
            index++;
            row = sheet.createRow(index);
            T t = it.next();
            if (t instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) t;
                ExcelUtils.write2Row(row, keys, map, pattern);
                continue;
            }
            ExcelUtils.write2Row(row, keys, t, pattern);

        }
        // 设定自动宽度
        for (int i = 0; i < headers.size(); i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) * 17 / 10);
        }
    }


    private static void write2Row(Row row, Set<String> keys, Map<String, Object> data, String pattern) {
        int cellNum = 0;
        //遍历列名
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String key = it.next();
            Object value = data.get(key);
            Cell cell = row.createCell(cellNum);
            setCellValue(cell, value, pattern);
            cellNum++;
        }
    }

    private static <T> void write2Row(Row row, Set<String> keys, T data, String pattern) throws Exception {
        int cellNum = 0;
        //遍历列名
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String key = it.next();
            PropertyDescriptor propertyDescriptor = BeanUtils.getPropertyDescriptor(data.getClass(), key);
            assert propertyDescriptor != null;
            Object value = propertyDescriptor.getReadMethod().invoke(data);
            Cell cell = row.createCell(cellNum);
            setCellValue(cell, value, pattern);
            cellNum++;
        }
    }


    private static void setCellValue(Cell cell, Object value, String pattern) {
        String textValue = null;
        if (value instanceof Integer) {
            int intValue = (Integer) value;
            cell.setCellValue(intValue);
        } else if (value instanceof Float) {
            float fValue = (Float) value;
            cell.setCellValue(fValue);
        } else if (value instanceof Double) {
            double dValue = (Double) value;
            cell.setCellValue(dValue);
        } else if (value instanceof Long) {
            long longValue = (Long) value;
            cell.setCellValue(longValue);
        } else if (value instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal) value;
            cell.setCellValue(decimal.doubleValue());
        } else if (value instanceof Boolean) {
            boolean bValue = (Boolean) value;
            cell.setCellValue(bValue);
        } else if (value instanceof Date) {
            Date date = (Date) value;
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            textValue = sdf.format(date);
        } else {
            // 其它数据类型都当作字符串简单处理
            textValue = value.toString();
        }
        if (textValue != null) {
            XSSFRichTextString richString = new XSSFRichTextString(textValue);
            cell.setCellValue(richString);
        }
    }

    public static <T> Collection<T> importExcel(InputStream inputStream, Map<String, String> headers, Class<T> clazz) throws Exception {
        return ExcelUtils.importExcel(inputStream, headers, clazz, "yyyy-MM-dd");
    }


    /**
     * 把Excel的数据封装成voList
     *
     * @param clazz       vo的Class
     * @param inputStream excel输入流
     * @param pattern     设定输入格式
     * @return voList
     * @throws RuntimeException
     */
    public static <T> Collection<T> importExcel(InputStream inputStream, Map<String, String> headers, Class<T> clazz, String pattern) throws Exception {
        List<T> list = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat(pattern);

        Map<String, String> reHeaders = ExcelUtils.reverseMapKeyValue(headers);
        Map<String, Integer> keyIndexMap = new HashMap<>();

        Workbook workBook = WorkbookFactory.create(inputStream);
        Sheet sheet = workBook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (row.getRowNum() == 0) {
                // 解析map用的key,就是excel标题行
                Iterator<Cell> cellIterator = row.cellIterator();
                int cellIndex = 0;
                while (cellIterator.hasNext()) {
                    String title = cellIterator.next().getStringCellValue();
                    String key = reHeaders.get(title);
                    if (StringUtils.isNotBlank(key)) {
                        keyIndexMap.put(key, cellIndex);
                    }
                    cellIndex++;
                }
                continue;
            }
            // 整行都空，就跳过
            boolean allRowIsNull = true;
            Iterator<Cell> cellIterator = row.cellIterator();
            while (cellIterator.hasNext()) {
                Object cellValue = getCellValue(cellIterator.next());
                if (cellValue != null) {
                    allRowIsNull = false;
                    break;
                }
            }
            if (allRowIsNull) {
//                LG.warn("Excel row " + row.getRowNum() + " all row value is null!");
                continue;
            }

            if (clazz.equals(Map.class)) {
                Map<String, Object> data = new HashMap<>();

                for (Map.Entry<String, Integer> entry : keyIndexMap.entrySet()) {
                    String key = entry.getKey();
                    Integer index = entry.getValue();
                    Cell cell = row.getCell(index);
                    // 判空
                    if (cell == null) {
                        data.put(key, null);
                    } else {
//                        cell.setCellType(CellType.STRING);
//                        String value = cell.getStringCellValue();
                        Object value = getCellValue(cell);
                        data.put(key, value);
                    }
                }
                list.add((T) data);

            } else {
                T data = clazz.newInstance();
                for (Map.Entry<String, Integer> entry : keyIndexMap.entrySet()) {
                    String key = entry.getKey();
                    Integer index = entry.getValue();
                    Cell cell = row.getCell(index);

                    PropertyDescriptor propertyDescriptor = BeanUtils.getPropertyDescriptor(clazz, key);
                    assert propertyDescriptor != null;
                    if (null != cell) {
                        Method writeMethod = propertyDescriptor.getWriteMethod();
                        Class<?> parameterType = writeMethod.getParameterTypes()[0];
                        Object value = getCellValue(cell, parameterType, sdf);
                        writeMethod.invoke(data, value);
                    }
                }
                list.add(data);
            }
        }
        return list;
    }

    private static Map<String, String> reverseMapKeyValue(Map<String, String> map) {
        HashMap<String, String> rmap = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            rmap.put(entry.getValue(), entry.getKey());
        }
        return rmap;
    }


    /**
     * 获取单元格值
     *
     * @param cell
     * @return
     */
    private static Object getCellValue(Cell cell) {
        if (cell == null
                || (cell.getCellTypeEnum() == CellType.STRING && StringUtils.isBlank(cell.getStringCellValue()))) {
            return null;
        }
        CellType cellType = cell.getCellTypeEnum();
        if (cellType == CellType.BLANK)
            return null;
        else if (cellType == CellType.BOOLEAN)
            return cell.getBooleanCellValue();
        else if (cellType == CellType.ERROR)
            return cell.getErrorCellValue();
        else if (cellType == CellType.FORMULA) {
            try {
                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    return cell.getNumericCellValue();
                }
            } catch (IllegalStateException e) {
                return cell.getRichStringCellValue();
            }
        } else if (cellType == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            } else {
                return cell.getNumericCellValue();
            }
        } else if (cellType == CellType.STRING) {
            return cell.getStringCellValue();
        }
        return null;
    }

    private static Object getCellValue(Cell cell, Class<?> clazz, SimpleDateFormat sdf) throws Exception {
        Object cellValue = getCellValue(cell);
        if (null == cellValue) {
            return null;
        }
        cell.setCellType(CellType.STRING);
        String stringValue = cell.getStringCellValue();
        if (clazz.equals(Long.class) && !(cellValue instanceof Long)) {
            return Double.valueOf(stringValue).longValue();
        }
        if (clazz.equals(Integer.class) && !(cellValue instanceof Integer)) {
            return Double.valueOf(stringValue).intValue();
        }
        if (clazz.equals(Double.class) && !(cellValue instanceof Double)) {
            return Double.valueOf(stringValue);
        }
        if (clazz.equals(Float.class) && !(cellValue instanceof Float)) {
            return Float.valueOf(stringValue);
        }
        if (clazz.equals(BigDecimal.class) && !(cellValue instanceof BigDecimal)) {
            return BigDecimal.valueOf(Double.parseDouble(stringValue));
        }
        if (clazz.equals(Date.class) && !(cellValue instanceof Date)) {
            return sdf.parse(stringValue);
        }
        if (clazz.equals(Boolean.class) && !(cellValue instanceof Boolean)) {
            return Boolean.valueOf(stringValue);
        }
        if (clazz.equals(String.class) && !(cellValue instanceof String)) {
            return stringValue;
        }
        return cellValue;
    }


    public static void main(String[] args) throws Exception {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("id", "主键");
        map.put("createTime", "创建时间");
        map.put("name", "名称");
        map.put("netWeight", "最小包装净重");
        map.put("unit", "单位");
        map.put("caseConvRate", "盒转化率");

//        Product product = new Product();
//        product.setId(0L);
//        product.setCreateTime(new Date());
//        product.setModifyTime(new Date());
//        product.setSyncode("");
//        product.setName("xxxxx");
//        product.setEnName("");
//        product.setPestName("");
//        product.setUnit("");
//        product.setNetWeight(new BigDecimal("10.235"));
//        product.setCaseConvRate(100);
//        product.setCaseWeight(new BigDecimal("10235.21"));
//        product.setCartonConvRate(0);
//        product.setCartonWeight(new BigDecimal("1023521.1"));
//        product.setPackageByCase("");
//        product.setPackageType("");
//        product.setValidPeriod(0);
//        product.setPestRegCertHolderName("");
//        product.setPestRegCertNumber("");
//        product.setPestRegCertSuffix("");
//        product.setLabelMaterialNumber("");
//        product.setProduceType("");
//        product.setProduceFactory("");
//        product.setProduceLine("");
//        product.setSpec("");
//        product.setWuliuCodeGenRate(0);
//        product.setCloudPageContent("");
//        product.setCategory("");
//        product.setRemark("");
//
//        ExcelUtils.exportExcel(map, Collections.singletonList(product), new FileOutputStream("E:/test.xlsx"), "yyyy-MM-dd HH:mm:ss");

        Collection<? extends Product> products = ExcelUtils.importExcel(new FileInputStream("E:/test.xlsx"), map, Product.class, "yyyy-MM-dd HH:mm:ss");
        System.err.println(products);
    }


}
