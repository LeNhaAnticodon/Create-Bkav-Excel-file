package com.lenha.createBKAVexcelFile.convert;

import com.lenha.createBKAVexcelFile.model.ExcelFile;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class ReadHoaDonExcelToBkavExcel {
    public static final String LOI_SAN_PHAM_CHUAN = "LOI_SAN_PHAM_CHUAN";
    public static final String LOI_DU_LIEU = "LOI_DU_LIEU";
    public static final String THANH_CONG = "THANH_CONG";
    private static Workbook excelHoaDon;
    private static final Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);

    public static String fileBkavName;

    /**
     * chuyển đổi file excel hóa đơn pancake sang hóa đơn bkav
     *
     * @param excelHoaDonFile       file hóa đơn
     * @param excelSanPhamChuanFile file sản phẩm chuẩn
     * @param excelBkavDir          địa chỉ thư mục chứa file bkav đầu ra
     * @param bkavExcelName         tên file bkav sẽ tạo
     * @param heSo                  hệ số dùng để nhập vào file bkav
     * @return kết quả chuyển file
     * @throws IOException đọc ghi file
     */
    public static String convertHoaDonExcelToBkavExcel(String excelHoaDonFile, String excelSanPhamChuanFile, String excelBkavDir, String bkavExcelName, double heSo, ObservableList<ExcelFile> excelFileNames) throws IOException {
        // xóa danh sách cũ trước khi thực hiện, tránh bị ghi chồng lên nhau
        excelFileNames.clear();
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
            if (fileBkavName.isBlank()) {
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
                // vì file bkav là file excel cũ đuôi xls nên cần tạo bằng HSSFWorkbook
                // nếu tạo bằng WorkbookFactory.create thì trong môi trường code vẫn chạy bình thường nhưng khi xuất ra file jar sẽ bị lỗi không thấy thư viện
                // khi này để tránh lỗi thì dùng HSSFWorkbook hoặc thêm thư viện vào maven
                /*
                    <dependencies>
                        <dependency>
                            <groupId>org.apache.poi</groupId>
                            <artifactId>poi</artifactId>
                            <version>5.2.5</version>
                        </dependency>
                        <dependency>
                            <groupId>org.apache.poi</groupId>
                            <artifactId>poi-ooxml</artifactId>
                            <version>5.2.5</version>
                        </dependency>
                    </dependencies>
                */
                excelBkav = new HSSFWorkbook(excelBkavStream);
            }

            // gọi hàm bắt đầu chuyển đổi dữ liệu từ file hóa đơn sang file đầu ra bkav vừa tạo
            String ketQua = ChuyenFileHoaDonSangFileBkav(excelHoaDon, excelBkav, heSo);

            try (FileOutputStream fileOut = new FileOutputStream(excelCopyPath)) {
                excelBkav.write(fileOut);

                excelBkav.close();
            }

            // thêm tên file vào list các sheet của file để hiển thị tên file
            excelFileNames.add(new ExcelFile(fileBkavName + ".xls", "", 0, 0));
            return ketQua;
        }


    }

    /**
     * chuyển đổi dữ liệu từ file hóa đơn sang file đầu ra bkav
     *
     * @param excelHoaDon excel hóa đơn
     * @param excelBkav   excel bkav
     * @param heSo        hệ số cần ghi vào cột trong excel bkav
     */
    private static String ChuyenFileHoaDonSangFileBkav(Workbook excelHoaDon, Workbook excelBkav, double heSo) {

        LinkedList<String[]> danhSachSP = new LinkedList<>();

        int[] cacCotThongTin = {2, 6, 7, 12, 15, 17};
        int soCotThongTin = cacCotThongTin.length;

        // gọi hàm lấy danh sách các sản phẩm trong file hóa đơn ban đầu cho vào list danhSachSP
        getDanhSachSanSP(danhSachSP, cacCotThongTin, soCotThongTin, excelHoaDon);

        Sheet sheet0Bkav = excelBkav.getSheetAt(0);
        int soSP = danhSachSP.size();

        System.out.println(soSP);
        for (int i = 0; i < soSP - 3; i++) {
            int rowWritingIndex = 6 + i;

            // chèm thêm một hàng xuống bên dưới
            insertRowBelow(sheet0Bkav, 6 + i);

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

        // thứ tự cột cần nhập dữ liệu 1-12-16-2-4-5, cột sdt của hóa đơn ban đầu không có nên
        for (int i = 0; i < soSP; i++) {
            Row rowSpI = sheet0Bkav.getRow(5 + i);
            String[] sanPham = danhSachSP.get(i);

            Cell oMaVanDon = rowSpI.getCell(1);
            Cell oHoTenNguoiMua = rowSpI.getCell(12);
            Cell oDiaChi = rowSpI.getCell(16);
            Cell oTenSanPham = rowSpI.getCell(2);
            Cell oSoLuong = rowSpI.getCell(4);
            Cell oDonGia = rowSpI.getCell(5);

            try {
                BigInteger maVanDonBig = new BigInteger(sanPham[0]);
                String maVanDon = sanPham[0];

                String tenNguoiMua = sanPham[1];
                String diaChi = sanPham[2];
                String tenSanPham = sanPham[3];
                double soLuong = Double.parseDouble(sanPham[4]);
                double donGia = Double.parseDouble(sanPham[5]) / 1.08;
                donGia = Math.round(donGia * 100.0) / 100.0;

                if (donGia == 0) {
                    tenSanPham = "Hàng tặng không thu tiền: " + tenSanPham;
                }

                oMaVanDon.setCellValue(maVanDon);
                oHoTenNguoiMua.setCellValue(tenNguoiMua);
                oDiaChi.setCellValue(diaChi);
                oTenSanPham.setCellValue(tenSanPham);
                oSoLuong.setCellValue(soLuong);
                oDonGia.setCellValue(donGia);

                // tự xử lý các ô tự tính
                // chỉ số các cột tự tính
                // stt = 0,
                // DonViTinh/ChietKhau = 3
                // ThanhTien = 6,
                // ThueSuat = 7,
                // TienThueGTGT= 8,
                // NgayThangNamHD = 9,
                // HinhThucTT = 18
                Cell oSTT = rowSpI.getCell(0);
                Cell oDonViTinh = rowSpI.getCell(3);
                Cell oThanhTien = rowSpI.getCell(6);
                Cell oThueSuat = rowSpI.getCell(7);
                Cell oGTGT = rowSpI.getCell(8);
                Cell oDate = rowSpI.getCell(9);
                Cell oCachTT = rowSpI.getCell(18);

                double thanhTien = donGia * soLuong;
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                String now = LocalDate.now().format(fmt);

                oSTT.setCellValue(i + 1);
                oDonViTinh.setCellValue("Kg");
                oThanhTien.setCellValue(Math.round(thanhTien));
                oThueSuat.setCellValue(heSo);
                oGTGT.setCellValue(Math.round(thanhTien * heSo));
                oDate.setCellValue(now);
                oCachTT.setCellValue("TM/CK");
            } catch (Exception e) {
                System.out.println("Lỗi chuyển dữ liệu thông tin sản phẩm");
                e.printStackTrace();
                // xóa hết các button, đổi alert sang dạng error rồi thêm lại 2 nút ok và cancel
                confirmAlert.getButtonTypes().clear();
                confirmAlert.setAlertType(Alert.AlertType.ERROR);
                confirmAlert.getButtonTypes().add(ButtonType.OK);

                confirmAlert.setTitle("Thông báo lỗi chuyển filr");
                confirmAlert.setHeaderText("Có sản phẩm có thông tin không đúng theo chuẩn số và chữ");
                confirmAlert.setContentText("Hãy sửa lại thông tin sản phẩm trong file đầu vào");
                confirmAlert.show();
                return LOI_DU_LIEU;
            }


        }


        return THANH_CONG;
    }

    private static void getDanhSachSanSP(LinkedList<String[]> danhSachSP, int[] cacCotThongTin, int soCotThongTin, Workbook excelHoaDon) {
        // lấy sheet 0 của excel hóa đơn
        Sheet sheet0HD = excelHoaDon.getSheetAt(0);
        // lấy hàng cuối chứa dữ liệu
        int latRowHD = sheet0HD.getLastRowNum();

        for (int i = 1; i <= latRowHD; i++) {
            Row rowI = sheet0HD.getRow(i);


            String[] sanPham = new String[soCotThongTin];

            if (rowI != null) {
                Cell oCotTenSanPham = rowI.getCell(12);
                if (oCotTenSanPham == null || oCotTenSanPham.toString().isBlank()) {
                    continue;
                }

                for (int j = 0; j < soCotThongTin; j++) {
                    int chiSoCotThongTin = cacCotThongTin[j];
                    Cell cotThongTinJ = rowI.getCell(chiSoCotThongTin);
                    if (cotThongTinJ == null) {
                        cotThongTinJ = rowI.createCell(chiSoCotThongTin);
                    }

                    String duLieu = cotThongTinJ.toString();

                    // nếu ô có giá trị rỗng thì copy giá trị của ô hàng trên cho ô này
                    if (duLieu.isBlank()) {
                        Row hangTren = sheet0HD.getRow(i - 1);
                        if (hangTren != null) {
                            Cell oHangTren = hangTren.getCell(chiSoCotThongTin);
                            duLieu = oHangTren.toString();
                        }
                    }
                    sanPham[j] = duLieu;
                }
            }

            danhSachSP.add(sanPham);
        }

//        danhSachSP.forEach(sanpham -> {
//            System.out.println(Arrays.toString(sanpham));
//        });
    }

    /**
     * check xem các tên trong hóa đơn ban đầu có khớp với các tên sản phẩm chuẩn không
     *
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
                    if (!tenTonTai) {
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

    public static void insertRowBelow(Sheet sheet, int rowIndex) {
        int lastRowNum = sheet.getLastRowNum();

        // Dịch tất cả các hàng từ rowIndex + 1 trở xuống xuống 1 dòng
        if (rowIndex + 1 <= lastRowNum) {
            sheet.shiftRows(rowIndex + 1, lastRowNum, 1, true, false);
        }

        // Tạo hàng mới ngay dưới rowIndex
        Row newRow = sheet.createRow(rowIndex + 1);

        // Nếu muốn giữ style của hàng trên
        Row sourceRow = sheet.getRow(rowIndex);
        if (sourceRow != null) {
            newRow.setHeight(sourceRow.getHeight());
            for (int i = sourceRow.getFirstCellNum(); i < sourceRow.getLastCellNum(); i++) {
                Cell oldCell = sourceRow.getCell(i);
                if (oldCell != null) {
                    Cell newCell = newRow.createCell(i);
                    newCell.setCellStyle(oldCell.getCellStyle());
                }
            }
        }
    }
}
