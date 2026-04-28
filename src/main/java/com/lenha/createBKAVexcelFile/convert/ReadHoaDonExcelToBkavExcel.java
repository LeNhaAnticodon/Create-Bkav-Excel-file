package com.lenha.createBKAVexcelFile.convert;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class ReadHoaDonExcelToBkavExcel {
    public static final String LOI_SAN_PHAM_CHUAN = "LOI_SAN_PHAM_CHUAN";
    public static final String THANH_CONG = "THANH_CONG";
    private static Workbook excelHoaDon;
    private static final Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);

    public static String fileBkavName;

    /**
     * chuyển đổi file excel hóa đơn pancake sang hóa đơn bkav
     * @param excelHoaDonFile file hóa đơn
     * @param excelSanPhamChuanFile file sản phẩm chuẩn
     * @param excelBkavDir địa chỉ thư mục chứa file bkav đầu ra
     * @param bkavExcelName tên file bkav sẽ tạo
     * @param heSo hệ số dùng để nhập vào file bkav
     * @return kết quả chuyển file
     * @throws IOException đọc ghi file
     */
    public static String convertHoaDonExcelToBkavExcel(String excelHoaDonFile, String excelSanPhamChuanFile, String excelBkavDir, String bkavExcelName, double heSo) throws IOException {
        // gán tên file bkav cho trường tên của class này
        fileBkavName = bkavExcelName;
        try (InputStream excelHoaDonStream = new FileInputStream(excelHoaDonFile)) {
            // tạo workbook của excel hóa đơn
            excelHoaDon = WorkbookFactory.create(excelHoaDonStream);

            // tạo workbook của excel sản phẩm chuẩn
            Workbook excelSanPhamChuan;
            try (InputStream excelSanPhamChuanStream = new FileInputStream(excelSanPhamChuanFile)) {
                excelSanPhamChuan = WorkbookFactory.create(excelSanPhamChuanStream);
            }
//            try (InputStream bkavExcelStream = new FileInputStream(bkavExcelName)) {
//                excelBkav = WorkbookFactory.create(bkavExcelStream);
//            }

            // lấy sheet 0 của excel sản phẩm chuẩn
            Sheet sheet0SPC = excelSanPhamChuan.getSheetAt(0);
            // tạo list chứa các tên sản phẩm chuẩn
            LinkedList<String> danhSachSPC = new LinkedList<>();
            // lấy hàng cuối chứa dữ liệu trong sheet 0 của excel sản phẩm chuẩn
            int lastRowSPC = sheet0SPC.getLastRowNum();
            // lấy ra các tên chuẩn cho vào list
            for (int i = 1; i <= lastRowSPC; i++) {
                Row rowI = sheet0SPC.getRow(i);
                if (rowI != null) {
                    Cell srcCell = rowI.getCell(0);
                    if (srcCell != null) {
                        danhSachSPC.add(srcCell.toString());
                    }
                    /*switch (srcCell.getCellType()) {
                        case NUMERIC:
                            if (DateUtil.isCellDateFormatted(srcCell)) {
                               danhSachSPC.add(String.valueOf(srcCell.getDateCellValue()));
                            } else {
                                danhSachSPC.add(String.valueOf(srcCell.getNumericCellValue()));
                            }
                            break;
                        case BOOLEAN:
                            danhSachSPC.add(String.valueOf(srcCell.getBooleanCellValue()));
                            break;
                        case FORMULA:
                            danhSachSPC.add(srcCell.getCellFormula());
                            break;
                        case BLANK:
                            danhSachSPC.add("");
                            break;
                        default:
                            // Những loại khác (nếu có) có thể thêm xử lý tương tự
                            danhSachSPC.add(srcCell.toString());
                            break;
                    }*/
                }
            }

            System.out.println(Arrays.toString(danhSachSPC.toArray()));

            // gọi hàm check xem các tên trong hóa đơn ban đầu có khớp với các tên sản phẩm chuẩn không
            ArrayList<Integer> cacHangTenLoi = checkTenChuan(danhSachSPC);

            System.out.println(Arrays.toString(cacHangTenLoi.toArray()));

            // nếu danh sách các hàng tên lỗi > 0 thì tức là có hàng lỗi
            // in lỗi thông báo sửa lại các tên rồi thoát hàm trả về nhãn lỗi
            if (cacHangTenLoi.size() > 0) {
                // xóa hết các button, đổi alert sang dạng error rồi thêm lại 2 nút ok và cancel
                confirmAlert.getButtonTypes().clear();
                confirmAlert.setAlertType(Alert.AlertType.ERROR);
                confirmAlert.getButtonTypes().add(ButtonType.OK);

                confirmAlert.setTitle("Thông báo lỗi tên");
                confirmAlert.setHeaderText("Có sản phẩm tên không đúng chuẩn");
                confirmAlert.setContentText("Hãy sửa lại tên sản phẩm tại các ô M" + Arrays.toString(cacHangTenLoi.toArray()) + " trong file đầu vào");
                confirmAlert.show();
                return LOI_SAN_PHAM_CHUAN;
            }

            // Đọc file mẫu excel bkav đầu ra từ resources rồi copy file ra địa chỉ của copyFilePath
            // nếu tên sản phẩm không điền thì cho nó tên mặc định bkav + ngày giờ
            if (fileBkavName.isBlank()){
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
                fileBkavName = "BKAV_" + LocalDateTime.now().format(fmt);
                System.out.println(fileBkavName);
            }
            // tạo link cho file đầu ra
            String excelCopyPath = excelBkavDir + "/" + fileBkavName + ".xls";
            // path chứa địa chỉ file bkav cần tạo sẽ được dán từ file copy
            Path copyFilePath = Paths.get(excelCopyPath);
            // Đọc file mẫu excel bkav đầu ra từ resources rồi copy file ra địa chỉ của copyFilePath
            String linkExcelBkavMauFile = "/com/lenha/createBKAVexcelFile/sampleFiles/file_bkav_chuan_dau_ra.xls";

            // Tạo đối tượng File đại diện cho file cần xóa
            File file = new File(excelCopyPath);
            // Kiểm tra nếu file tồn tại thì xóa nó
            // vì nếu file đang được mở thì không thể ghi đè
            // xóa xong file thì có thể ghi lại file mới mà không bị lỗi không thể ghi đè
            if (file.exists()) {
                if (file.delete()) {
                    System.out.println("File đã được xóa thành công.");
                } else {
                    System.out.println("Xóa file thất bại.");
                }
            }
            // tạo file đầu ra copy từ file mẫu
            try (InputStream sourceFile = ReadPDFToExcel.class.getResourceAsStream(linkExcelBkavMauFile)) {
                if (sourceFile == null) {
                    throw new IOException("File mẫu không tồn tại trong JAR ứng dụng");
                }
                Files.copy(sourceFile, copyFilePath);
            }

            // đến đây nếu không có lỗi nữa thì tạo workbook của file bkav đầu ra excelBkav
            Workbook excelBkav;
            try (InputStream excelBkavStream = new FileInputStream(excelCopyPath)) {
                excelBkav = WorkbookFactory.create(excelBkavStream);
            }

            // gọi hàm bắt đầu chuyển đổi dữ liệu từ file hóa đơn sang file đầu ra bkav vừa tạo
            ChuyenFileHoaDonSangFileBkav(excelHoaDon, excelBkav, heSo);

            try (FileOutputStream fileOut = new FileOutputStream(excelCopyPath)) {
                excelBkav.write(fileOut);

                excelBkav.close();
            }
        }

        return THANH_CONG;

    }

    /**
     * chuyển đổi dữ liệu từ file hóa đơn sang file đầu ra bkav
     * @param excelHoaDon excel hóa đơn
     * @param excelBkav excel bkav
     * @param heSo hệ số cần ghi vào cột trong excel bkav
     */
    private static void ChuyenFileHoaDonSangFileBkav(Workbook excelHoaDon, Workbook excelBkav, double heSo) {

        LinkedList<String[]> danhSachSP = new LinkedList<>();

        int[] cacCotThongTin = {2, 5, 6, 7, 12, 15, 17};
        int soCotThongTin = cacCotThongTin.length;

        // gọi hàm lấy danh sách các sản phẩm trong file hóa đơn ban đầu cho vào list danhSachSP
        getDanhSachSanSP(danhSachSP, cacCotThongTin, soCotThongTin, excelHoaDon);

        Sheet sheet0Bkav = excelBkav.getSheetAt(0);
        int soSP = danhSachSP.size();

        System.out.println(soSP);
        for (int i = 0; i < soSP - 3; i++) {
            int rowWritingIndex = 6 + i;
            Row rowWriting = sheet0Bkav.getRow(rowWritingIndex);

            Row underRowWriting = sheet0Bkav.getRow(rowWritingIndex + 1);
            int rowWritingHeight = rowWriting.getHeight();
            if (underRowWriting == null) {
                underRowWriting = sheet0Bkav.createRow(rowWritingIndex + 1);
            }

            // copy cả chiều cao dòng của dòng bên trên
            underRowWriting.setHeight((short) rowWritingHeight);
            for (int k = 0; k < 30; k++) {
                copyRowCellWithFormulaUpdate(rowWriting.getCell(k), underRowWriting.getCell(k), 1);
            }
        }
        

    }

    private static void getDanhSachSanSP(LinkedList<String[]> danhSachSP, int[] cacCotThongTin, int soCotThongTin, Workbook excelHoaDon){
        // lấy sheet 0 của excel hóa đơn
        Sheet sheet0HD = excelHoaDon.getSheetAt(0);
        // lấy hàng cuối chứa dữ liệu
        int latRowHD = sheet0HD.getLastRowNum();

        for (int i = 1; i <= latRowHD; i++) {
            Row rowI = sheet0HD.getRow(i);
            String[] sanPham = new String[soCotThongTin];

            if (rowI != null) {
                for (int j = 0; j < soCotThongTin; j++) {
                    int chiSoCotThongTin = cacCotThongTin[j];
                    Cell cotThongTinJ = rowI.getCell(chiSoCotThongTin);
                    if (cotThongTinJ == null) {
                        cotThongTinJ = rowI.createCell(chiSoCotThongTin);
                    }

                    String duLieu = cotThongTinJ.toString();

                    // nếu ô có giá trị rỗng thì xem ô mã vận đơn của hàng trên có giống hàng này không
                    // nếu có thì copy giá trị của ô hàng trên cho ô này
                    if (duLieu.isBlank()){
                        Row hangTren = sheet0HD.getRow(i - 1);
                        if (hangTren != null){
                            Cell oCot2 = rowI.getCell(2);
                            Cell oCot2HangTren = hangTren.getCell(2);
                            if (oCot2 != null && oCot2HangTren != null){
                                if (oCot2.toString().equals(oCot2HangTren.toString())){
                                    Cell oHangTren = hangTren.getCell(chiSoCotThongTin);
                                    duLieu = oHangTren.toString();
                                }
                            }
                        }
                    }
                    sanPham[j] = duLieu;
                }
            }

            danhSachSP.add(sanPham);
        }

        danhSachSP.forEach(sanpham -> {
            System.out.println(Arrays.toString(sanpham));
        });
    }

    /**
     * check xem các tên trong hóa đơn ban đầu có khớp với các tên sản phẩm chuẩn không
     * @param danhSachSPC danh sách các sản phẩm chuẩn
     * @return danh sách chỉ số các hàng chứa tên sai chuẩn
     */
    private static ArrayList<Integer> checkTenChuan(LinkedList<String> danhSachSPC) {
        // lấy sheet 0 của excel hóa đơn
        Sheet sheet0HD = excelHoaDon.getSheetAt(0);
        // lấy hàng cuối chứa dữ liệu
        int latRowCotSPHoaDon = sheet0HD.getLastRowNum();
        // tạo list chứa chỉ số các hàng có tên lỗi
        ArrayList<Integer> cacHangTenLoi = new ArrayList<>();
        // lặp qua các tên và tìm xem tên nào lỗi thì cho chỉ số hàng chứa nó vào list
        for (int i = 1; i <= latRowCotSPHoaDon; i++) {
            Row rowI = sheet0HD.getRow(i);
            if (rowI != null) {
                // lấy tên tại cột 12
                Cell srcCell = rowI.getCell(12);
                if (srcCell != null) {
                    // lấy ra tên sản phẩm
                    String tenSP = srcCell.toString();
                    // biến nhớ xác định tên có tồn tại trong list tên chuẩn không
                    boolean tenTonTai = false;
                    // lặp qua list tên chuẩn và kiểm tra tên sản phẩm có nằm trong đó không.
                    // nếu có thì gán tenTonTai là true rồi thoát
                    // nếu kết thúc vòng lặp biến nhớ vần false thì tức là tên sản phẩm không khớp tên chuẩn
                    // thì thêm chỉ số hàng của sản phẩm này vào list các hàng tên lỗi
                    for (String tenSPC : danhSachSPC) {
                        if (tenSPC.equals(tenSP)) {
                            tenTonTai = true;
                            break;
                        }
                    }
                    // nếu kết thúc vòng lặp biến nhớ vần false thì tức là tên sản phẩm không khớp tên chuẩn
                    // thì thêm chỉ số hàng của sản phẩm này vào list các hàng tên lỗi
                    if (!tenTonTai){
                        cacHangTenLoi.add(i + 1);
                    }
                }
            }

        }
        // trả về danh sách chỉ số các hàng chứa tên sai chuẩn
        return cacHangTenLoi;
    }

    /**
     * copy công thức từ srcCell vào destCell và thay đổi công thức theo hàng
     * của destCell cho phù hợp dựa vào khoảng cách hàng giữa 2 cell bằng tham số shiftRows
     *
     * @param srcCell   cell gốc
     * @param destCell  cell copy từ cell gốc
     * @param shiftRows khảng cách hàng giữa cell gốc và cell copy
     */
    private static void copyRowCellWithFormulaUpdate(Cell srcCell, Cell destCell, int shiftRows) {
        if (destCell == null) {
            destCell = srcCell.getSheet().getRow(srcCell.getRowIndex() + shiftRows).createCell(srcCell.getColumnIndex());
        }
        destCell.setCellStyle(srcCell.getCellStyle());
        switch (srcCell.getCellType()) {
            case STRING:
                destCell.setCellValue(srcCell.getStringCellValue());
                break;
            case NUMERIC:
                destCell.setCellValue(srcCell.getNumericCellValue());
                break;
            case BOOLEAN:
                destCell.setCellValue(srcCell.getBooleanCellValue());
                break;
            case FORMULA:
                String formula = srcCell.getCellFormula();
                StringBuilder updatedFormula = new StringBuilder(updateRowFormula(formula, shiftRows));
                updatedFormula = new StringBuilder(updatedFormula.toString().replaceAll("SUN", "SUM"));


                destCell.setCellFormula(updatedFormula.toString());
                break;
            case BLANK:
                destCell.setBlank();
                break;
            default:
                break;
        }
    }

    private static String updateRowFormula(String formula, int shiftRows) {
        StringBuilder updatedFormula = new StringBuilder();
        int length = formula.length();
        boolean isAbsoluteColumn = false;
        boolean isAbsoluteRow = false;

        for (int i = 0; i < length; i++) {
            char c = formula.charAt(i);
            if (c == '$') {
                if (i + 1 < length && Character.isLetter(formula.charAt(i + 1))) {
                    isAbsoluteColumn = true;
                    updatedFormula.append(c);
                } else if (i + 1 < length && Character.isDigit(formula.charAt(i + 1))) {
                    isAbsoluteRow = true;
                    updatedFormula.append(c);
                }
            } else if (Character.isLetter(c)) {
                StringBuilder column = new StringBuilder();
                while (i < length && Character.isLetter(formula.charAt(i))) {
                    column.append(formula.charAt(i));
                    i++;
                }
                if (isAbsoluteColumn) {
                    updatedFormula.append(column.toString());
                } else {
                    updatedFormula.append(column.toString());
                }
                isAbsoluteColumn = false;
                i--; // Adjust for the increment in the loop
            } else if (Character.isDigit(c)) {
                StringBuilder row = new StringBuilder();
                while (i < length && Character.isDigit(formula.charAt(i))) {
                    row.append(formula.charAt(i));
                    i++;
                }
                int rowIndex = Integer.parseInt(row.toString());
                if (!isAbsoluteRow) {
                    rowIndex += shiftRows;
                }
                updatedFormula.append(rowIndex);
                isAbsoluteRow = false;
                i--; // Adjust for the increment in the loop
            } else {
                updatedFormula.append(c);
            }
        }
        return updatedFormula.toString();
    }
}
