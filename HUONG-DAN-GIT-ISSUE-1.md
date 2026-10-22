# HƯỚNG DẪN CHI TIẾT THỰC HIỆN ISSUE 1: PUSH PULL REQUEST VÀ MERGE VÀO REPO
> **Dự án:** Hệ thống Nhận dạng Hình ảnh Phân tán (Nhóm 01 - Lớp 23DTHE4, 23DTHA5)  
> **Người thực hiện:** Trần Công Tiến (MSSV: `2380614894`)  
> **Đầu việc:** Issue 1 - Khung Maven + Giao thức mạng + Stub hợp đồng  
> **Tài liệu căn cứ:** `QUY TRÌNH VÀ RÀNG BUỘC THỰC HIỆN DỰ ÁN TRÊN GIT/GITHUB.pdf`

---

## MỤC LỤC
1. [Thông tin chuẩn hóa định danh và quy ước](#1-thông-tin-chuẩn-hóa-định-danh-và-quy-ước)
2. [Cảnh báo vệ sinh mã nguồn (Cực kỳ quan trọng - Tránh trừ 20% điểm)](#2-cảnh-báo-vệ-sinh-mã-nguồn)
3. [Quy trình 8 bước: Từ Tạo Issue đến Merge vào nhánh chính](#3-quy-trình-8-bước-thực-hiện-chi-tiết)
   - [Bước 1: Tạo Issue #1 trên GitHub/GitLab](#bước-1-tạo-issue-1-trên-githubgitlab)
   - [Bước 2: Khởi tạo Local Repository và tạo nhánh tính năng](#bước-2-khởi-tạo-local-repository-và-tạo-nhánh-tính-năng)
   - [Bước 3: Chạy kiểm thử xác minh và chụp ảnh bằng chứng](#bước-3-chạy-kiểm-thử-xác-minh-và-chụp-ảnh-bằng-chứng)
   - [Bước 4: Commit theo chuẩn Conventional Commits](#bước-4-commit-theo-chuẩn-conventional-commits)
   - [Bước 5: Push nhánh tính năng lên Remote](#bước-5-push-nhánh-tính-năng-lên-remote)
   - [Bước 6: Tạo Pull Request / Merge Request](#bước-6-tạo-pull-request--merge-request)
   - [Bước 7: Review chéo (Peer Review) và Merge](#bước-7-review-chéo-peer-review-và-merge)
   - [Bước 8: Cập nhật Bảng ma trận đóng góp (README.md)](#bước-8-cập-nhật-bảng-ma-trận-đóng-góp-readmemd)
4. [Bảng lệnh Git tra cứu nhanh](#4-bảng-lệnh-git-tra-cứu-nhanh)
5. [Xử lý các tình huống thường gặp](#5-xử-lý-các-tình-huống-thường-gặp)

---

## 1. THÔNG TIN CHUẨN HÓA ĐỊNH DANH VÀ QUY ƯỚC

Theo quy định bắt buộc tại Mục 1, 3, 4 trong file PDF:

| Mục | Quy định | Thông tin áp dụng cho bạn |
| :--- | :--- | :--- |
| **MSSV & Họ tên** | Khớp danh sách lớp | `2380614894` - `TranCongTien` |
| **Git user.name** | `MSSV_HoVaTen` | `2380614894_TranCongTien` *(Đã cấu hình)* |
| **Git user.email** | Email sinh viên / cá nhân | `congtien1202@gmail.com` *(Đã cấu hình)* |
| **Số Issue** | Số thứ tự trên Task Board | `1` (Issue #1) |
| **Tên nhánh (Branch)** | `feature/<MSSV>-issue-<Số>-<Tên_ngắn_gọn>` | `feature/2380614894-issue-1-khung-maven-giao-thuc` |
| **Commit Type** | Conventional: `<loại>: <mô tả>` | `feat: khoi tao khung maven, giao thuc mang va cac stub hop dong` |
| **Nhánh chính** | Protected (`main` hoặc `master`) | Tuyệt đối **không** push trực tiếp |

---

## 2. CẢNH BÁO VỆ SINH MÃ NGUỒN

> ⚠️ **ĐIỀU KHOẢN KỶ LUẬT (Mục 7 PDF):**  
> **Vi phạm quy tắc vệ sinh kho mã nguồn (Push file rác, file build, file nhị phân lớn): TRỪ 20% TỔNG ĐIỂM NHÓM.**

### Danh sách các thư mục & file TUYỆT ĐỐI KHÔNG COMMIT:
1. Thư mục `chuong-trinh/` (chứa `NhanDangAnh.exe`, các file `.jar` 70MB+).
2. Thư mục build: `target/`, `target-*/`, `dist/`.
3. File cấu hình cá nhân của IDE: `.idea/`, `.vscode/`, `*.iml`, `nbproject/private/`.
4. File log & cache mô hình: `logs/`, `.djl.ai/`.

### File `.gitignore` chuẩn cho thư mục gốc:
Đảm bảo tại thư mục gốc repository có file `.gitignore` chứa tối thiểu các dòng sau:
```gitignore
# Thư mục chứa bản build và file nhị phân lớn (CỰC KỲ QUAN TRỌNG)
target/
target-*/
chuong-trinh/
*.jar
*.exe
*.zip

# IDE & Cấu hình máy cá nhân
.idea/
*.iml
.vscode/
nbproject/private/
nbactions*.xml

# Log và Cache
logs/
.djl.ai/
*.log
```

---

## 3. QUY TRÌNH 8 BƯỚC THỰC HIỆN CHI TIẾT

### BƯỚC 1: TẠO ISSUE #1 TRÊN GITHUB / GITLAB
*(Mục 2 trong PDF: Mọi việc đều phải tạo Issue trước khi code/tích hợp)*

1. Truy cập vào Repository của nhóm trên trình duyệt web (GitHub hoặc GitLab).
2. Vào tab **Issues** → Bấm nút **New Issue**.
3. Điền thông tin:
   - **Title (Tiêu đề):** `Khung Maven + giao thức mạng + stub hợp đồng`
   - **Assignees:** Gán cho chính bạn (`TranCongTien`). *(Quy định: Đúng 1 người chịu trách nhiệm chính).*
   - **Labels:** Chọn `enhancement` hoặc `backend`.
   - **Milestone:** Chọn `Tuần 1` (nếu nhóm đã tạo Milestone).
   - **Description (Mô tả):**
     ```markdown
     ## Mục tiêu
     Thiết lập cấu trúc dự án Maven đa module (common, server, client), giao thức truyền thông nhị phân Message (TCP) và các lớp hợp đồng (ObjectDetector, ServerConnection, BoxRenderer, HistoryStore, ResultExporter) dạng stub.

     ## Danh sách công việc
     - [x] Tạo khung Maven multi-module chạy được mvnw
     - [x] Cài đặt bộ mã hóa/giải mã Message nhị phân trong module common
     - [x] Viết unit tests kiểm tra tính toàn vẹn của giao thức (20 tests passed)
     - [x] Định nghĩa các giao diện (interfaces/stubs) cho các thành viên khác triển khai song song
     ```
4. Bấm **Submit new issue**. Lưu ý số của Issue (thường là **#1**).

---

### BƯỚC 2: KHỞI TẠO LOCAL REPOSITORY VÀ TẠO NHÁNH TÍNH NĂNG

Mở cửa sổ **PowerShell** hoặc **Git Bash** tại máy tính của bạn:

#### Trường hợp A: Repo đã tạo trên GitHub/GitLab nhưng thư mục này chưa liên kết Git
*(Hiện tại thư mục `NhanDangAnh-Nhom01` trên máy bạn chưa có `.git`)*

1. Chuyển vào thư mục mã nguồn dự án:
   ```powershell
   cd c:\Users\ADMIN\Downloads\NhanDangAnh-Nhom01
   ```

2. Khởi tạo Git:
   ```powershell
   git init -b main
   ```
   *(Nếu Git phiên bản cũ dùng master thì gõ: `git init` rồi `git branch -M main`)*

3. Thêm địa chỉ Remote của repo nhóm trên GitHub (nếu chưa thêm):
   ```powershell
   git remote add origin https://github.com/congtien1202-lab/NHOM1_NetworkProgramming_PR.git
   ```

4. Kiểm tra remote:
   ```powershell
   git remote -v
   ```

5. Kéo dữ liệu từ remote về (Khắc phục lỗi đè file `README.md`):
   > 💡 **Xử lý lỗi:** Nếu terminal báo `error: The following untracked working tree files would be overwritten by merge: README.md`:
   > Nguyên nhân là do trên GitHub đã có file `README.md` từ trước, trong khi máy cục bộ cũng có `README.md` chưa được track.
   
   Chạy 2 lệnh sau:
   ```powershell
   # Bước 5.1: Đổi tên tạm thời file README.md cục bộ
   Rename-Item README.md README.md.bak

   # Bước 5.2: Kéo main từ remote về
   git pull origin main --allow-unrelated-histories
   ```

6. **Tạo và chuyển sang nhánh tính năng (Feature Branch) đúng chuẩn:**
   ```powershell
   git checkout -b feature/2380614894-issue-1-khung-maven-giao-thuc
   ```
   > Cú pháp bắt buộc: `feature/<MSSV>-issue-<Số_Issue>-<Tên_ngắn_gọn>`

7. **Khôi phục lại nội dung `README.md` chuẩn (chứa Bảng đóng góp nhóm):**
   ```powershell
   Move-Item -Force README.md.bak README.md
   ```

---

### BƯỚC 3: CHẠY KIỂM THỬ XÁC MINH VÀ CHỤP ẢNH BẰNG CHỨNG
*(Mục 5 trong PDF: Yêu cầu đính kèm ảnh chụp màn hình chứng minh code đã chạy thành công)*

1. Chạy test mã nguồn để đảm bảo toàn bộ unit test thành công:
   ```powershell
   cd ma-nguon
   .\mvnw.cmd test
   ```
2. Kết quả xuất hiện:
   ```text
   [INFO] BUILD SUCCESS
   ```
3. **Chụp màn hình:** Nhấn tổ hợp phím `Windows + Shift + S` chụp lại cửa sổ console hiển thị chữ `BUILD SUCCESS` và các test case `common` chạy thành công. Lưu ảnh ra desktop để lát nữa chèn vào Pull Request.

---

### BƯỚC 4: COMMIT THEO CHUẨN CONVENTIONAL COMMITS
*(Mục 4 trong PDF: Cấu trúc <loại>: <mô tả ngắn gọn>)*

1. Đứng tại thư mục gốc dự án (`NhanDangAnh-Nhom01`):
   ```powershell
   cd c:\Users\ADMIN\Downloads\NhanDangAnh-Nhom01
   ```

2. Kiểm tra các file sẽ được đưa vào Git:
   ```powershell
   git status
   ```
   > ⚠️ **KIỂM TRA KỸ:** Đảm bảo **KHÔNG CÓ** thư mục `chuong-trinh/` hay bất kỳ file `.jar`, `.exe`, `target/` nào nằm trong danh sách "Untracked files".

3. Thêm các file mã nguồn và tài liệu vào Stage:
   ```powershell
   git add ma-nguon/ .gitignore CLAUDE.md HUONG-DAN-SU-DUNG.md
   ```

4. Thực hiện commit đúng chuẩn:
   ```powershell
   git commit -m "feat: khoi tao khung maven, giao thuc mang va cac lop stub hop dong"
   ```

---

### BƯỚC 5: PUSH NHÁNH TÍNH NĂNG LÊN REMOTE
*(Tuyệt đối không push lên `main`)*

1. Đẩy nhánh cá nhân lên GitHub/GitLab:
   ```powershell
   git push -u origin feature/2380614894-issue-1-khung-maven-giao-thuc
   ```
2. Nếu Git yêu cầu đăng nhập, chọn **Sign in with your browser** hoặc dùng Personal Access Token (PAT).
3. Sau khi push thành công, terminal sẽ hiện liên kết tạo Pull Request.

---

### BƯỚC 6: TẠO PULL REQUEST / MERGE REQUEST
*(Mục 5 trong PDF: Ràng buộc mô tả chi tiết, ảnh chứng minh)*

1. Truy cập vào Repository nhóm trên GitHub.
2. Bạn sẽ thấy thanh màu vàng thông báo nhánh vừa push kèm nút:
   👉 **Compare & pull request** (bấm vào nút này).
3. Cấu hình PR:
   - **Base branch (nhánh đích):** `main`
   - **Compare branch (nhánh nguồn):** `feature/2380614894-issue-1-khung-maven-giao-thuc`
   - **Title:** `feat: khởi tạo khung maven, giao thức mạng và stub hợp đồng (Issue #1)`
4. **Description (Mô tả) - Điền theo mẫu chuẩn bắt buộc:**

```markdown
### 1. Đã làm gì (Summary of Changes)?
- Thiết lập cấu trúc Maven đa module: `common`, `server`, `client`.
- Xây dựng giao thức mạng nhị phân `Message` (TCP) trong package `common`:
  - Các loại thông điệp: Hello, HelloOk, DetectRequest, DetectResult, ErrorReply, Ping, Pong, Bye.
  - Bộ mã hóa/giải mã `ProtocolCodec` cùng cơ chế kiểm soát lỗi `ErrorCode`.
- Xây dựng các lớp hợp đồng (stub) để 3 thành viên còn lại có thể phát triển song song:
  - `ObjectDetector` cho nhóm AI/DJL.
  - `ServerConnection` cho nhóm Client UI.
  - `HistoryStore` và `ResultExporter` cho nhóm Lịch sử & Xuất file.
- Viết 20 unit tests xác minh tính toàn vẹn của giao thức mạng.

### 2. Có ảnh hưởng đến các module khác không?
- Không. Đây là commit nền tảng (Phase 0), cung cấp giao diện và stub để các module khác bắt đầu triển khai độc lập mà không bị xung đột.

### 3. Bằng chứng kiểm thử (Screenshot)
*(Kéo thả ảnh chụp màn hình chạy test `mvnw test` BUILD SUCCESS vào đây)*

Closes #1
```

5. Gán người Review:
   - Tại mục **Reviewers** bên phải: Chọn 1 hoặc nhiều bạn trong nhóm (ví dụ: `HoNgocLinh`, `NguyenVanAn`, hoặc `VuongThiMyHoa`).
   - Tại mục **Assignees**: Chọn chính bạn (`TranCongTien`).
6. Bấm nút **Create pull request**.

---

### BƯỚC 7: REVIEW CHÉO (PEER REVIEW) VÀ MERGE

> ⚠️ **QUY ĐỊNH NGHIÊM NGẶT (Mục 5 PDF):**  
> **Nghiêm cấm hành vi tự tạo MR và tự Approve cho chính mình! Phải có ÍT NHẤT 1 thành viên khác vào duyệt.**

1. **Gửi link PR cho đồng đội:**
   - Bạn copy link PR vừa tạo gửi vào nhóm Zalo / Discord / Messenger của nhóm 01.
   - Nhờ một bạn (ví dụ Hồ Ngọc Linh hoặc Nguyễn Văn An) vào kiểm tra.

2. **Các bước đồng đội cần thực hiện (Người Review):**
   - Vào tab **Files changed** trên PR để xem các thay đổi mã nguồn.
   - Bấm nút xanh **Review changes** ở góc trên bên phải.
   - Viết nhận xét (Ví dụ: *"Code chuẩn, các stub rõ ràng, test đã pass đầy đủ."*).
   - Tích chọn **Approve**.
   - Bấm **Submit review**.

3. **Thực hiện Merge:**
   - Sau khi PR đã có đủ trạng thái **Approved**:
   - Nhóm trưởng hoặc bạn có thể bấm nút **Merge pull request** → chọn **Confirm merge**.
   - Sau khi merge xong, bấm **Delete branch** trên GitHub để dọn dẹp nhánh tính năng.

---

### BƯỚC 8: CẬP NHẬT BẢNG MA TRẬN ĐÓNG GÓP (README.md)
*(Mục 6 trong PDF: Duy trì bảng đóng góp để chấm điểm bảo vệ cuối kỳ)*

Sau khi PR được merge thành công, mở file `README.md` ở thư mục gốc repository để cập nhật dòng đóng góp của bạn:

```markdown
## BẢNG MA TRẬN THỐNG KÊ ĐÓNG GÓP CỦA THÀNH VIÊN

| MSSV | Họ và Tên | Công việc đã thực hiện | Merge Requests |
| :--- | :--- | :--- | :--- |
| **2380614894** | **Trần Công Tiến** | Khởi tạo khung Maven đa module, giao thức mạng TCP nhị phân, bộ mã hóa ProtocolCodec và các lớp stub hợp đồng điều phối | **#1** |
| 2380601216 | Hồ Ngọc Linh | Xây dựng bộ nhận dạng AI với Deep Java Library (DJL) và SSD-MobileNet | |
| 2380600020 | Nguyễn Văn An | Xây dựng giao diện Client Swing, kết nối Socket và hiển thị Bounding Box | |
| 2380600688 | Vương Thị Mỹ Hoa | Xây dựng tính năng Lịch sử nhận dạng, Xuất file PNG/CSV và tài liệu triển khai | |
```

---

## 4. BẢNG LỆNH GIT TRA CỨU NHANH

| Lệnh | Ý nghĩa |
| :--- | :--- |
| `git status` | Xem trạng thái các file thay đổi / chưa theo dõi |
| `git branch -a` | Xem tất cả các nhánh hiện có |
| `git checkout -b <tên-nhánh>` | Tạo và chuyển sang nhánh mới |
| `git add <file/thư-mục>` | Thêm file vào danh sách chuẩn bị commit (Stage) |
| `git commit -m "<thông-điệp>"` | Ghi lại thay đổi với thông điệp theo chuẩn |
| `git push -u origin <tên-nhánh>` | Đẩy nhánh lên GitHub lần đầu tiên |
| `git pull origin main` | Kéo cập nhật mới nhất từ nhánh chính về máy |

---

## 5. XỬ LÝ CÁC TÌNH HUỐNG THƯỜNG GẶP

### 1. Lỡ gõ `git add .` và bị dính file `.jar` hoặc thư mục `chuong-trinh/`:
Hủy đưa các file nhị phân vào stage ngay lập tức:
```powershell
git reset
```
Sau đó kiểm tra lại file `.gitignore` để chắc chắn đã chặn các file nhị phân.

### 2. Bị lỗi `Protected Branch` khi push:
Lỗi này xảy ra khi bạn cố tình push trực tiếp vào `main`. Hãy đảm bảo bạn đang ở nhánh `feature/2380614894-issue-1-khung-maven-giao-thuc`:
```powershell
git branch
# Nếu đang ở main, chuyển ngay sang nhánh feature:
git checkout -b feature/2380614894-issue-1-khung-maven-giao-thuc
```

### 3. Đồng bộ lại máy sau khi PR đã merge trên GitHub:
Sau khi PR được merge trên GitHub, chuyển về nhánh `main` và kéo code mới về:
```powershell
git checkout main
git pull origin main
```
Xóa nhánh feature cục bộ đã merge để giữ sạch máy:
```powershell
git branch -d feature/2380614894-issue-1-khung-maven-giao-thuc
```

### 4. Lỗi: `error: The following untracked working tree files would be overwritten by merge: README.md`:
- **Nguyên nhân:** Trên remote GitHub đã có sẵn file `README.md`, trong khi thư mục máy bạn cũng có file `README.md` mới tạo nhưng chưa được commit.
- **Cách khắc phục:**
  1. Đổi tên tạm thời file trên máy:
     ```powershell
     Rename-Item README.md README.md.bak
     ```
  2. Kéo dữ liệu về:
     ```powershell
     git pull origin main --allow-unrelated-histories
     ```
  3. Tạo nhánh feature:
     ```powershell
     git checkout -b feature/2380614894-issue-1-khung-maven-giao-thuc
     ```
  4. Đổi lại file README đầy đủ bảng đóng góp:
     ```powershell
     Move-Item -Force README.md.bak README.md
     ```

---
*Tài liệu được biên soạn tự động dựa trên quy định chuẩn Git/GitHub của đồ án môn Lập trình mạng.*
