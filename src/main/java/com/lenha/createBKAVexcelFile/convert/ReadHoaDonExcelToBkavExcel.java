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
}
