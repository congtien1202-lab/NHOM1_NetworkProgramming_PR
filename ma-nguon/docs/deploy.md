# Hướng dẫn build và triển khai

## 1. Build

Windows (PowerShell, tại thư mục gốc dự án):

```powershell
.\mvnw.cmd -q package
```

Linux: `./mvnw -q package`. Lệnh tạo hai file jar chạy độc lập (đã gồm mọi thư viện):

- `server/target/server-1.0-SNAPSHOT-all.jar`
- `client/target/client-1.0-SNAPSHOT-all.jar`

Chạy Server:

```
java -jar server-1.0-SNAPSHOT-all.jar --gui=false --detector=djl --port=5000 --maxClients=20
```

| Tham số | Ý nghĩa | Mặc định |
|---|---|---|
| `--port` | Cổng lắng nghe | 5000 |
| `--detector` | `djl` (mô hình thật) hoặc `fake` (kết quả giả để thử mạng) | `fake` |
| `--gui` | `false` để chạy không cửa sổ (VPS) | `true` |
| `--maxClients` | Số Phiên tối đa cùng lúc; vượt thì Client mới nhận "Server bận" | 20 |

Phát cho người dùng:

- **Không cần cài Java (khuyến nghị):** chạy `powershell -ExecutionPolicy Bypass -File scripts\package-client.ps1`, gửi file `dist\NhanDangAnh-windows.zip`. Người nhận giải nén rồi chạy `NhanDangAnh\NhanDangAnh.exe`.
- **Đã có Java 17+:** gửi file jar Client, chạy `java -jar client-1.0-SNAPSHOT-all.jar`.

Client mặc định kết nối `ai.devlinh.com:5000`. Khi chạy thử với Server trên máy mình: `java -Dimagerec.host=localhost -jar client-1.0-SNAPSHOT-all.jar` hoặc sửa ô host trên giao diện.

## 2. Demo trong LAN / máy Windows

1. Chạy Server trên một máy Windows. Lần đầu, Windows Defender Firewall hiện hộp thoại hỏi quyền cho `java.exe`: chọn cho phép trên mạng **Private**. Nếu lỡ bỏ qua, thêm rule inbound TCP 5000 (Windows Defender Firewall with Advanced Security, Inbound Rules, New Rule, Port, TCP 5000, Allow). Thiếu bước này thì Client ở máy khác không kết nối được dù cùng mạng.
2. Xem IP LAN của máy Server bằng `ipconfig` (dòng IPv4 Address).
3. Trên Client nhập IP đó và cổng 5000.

## 3. Phương án khuyến nghị: VPS

Yêu cầu: VPS Ubuntu, từ 2 GB RAM, có IP tĩnh.

1. **Cài Java**: `sudo apt update && sudo apt install -y openjdk-17-jre-headless` (hoặc bản mới hơn).
2. **Đưa jar lên VPS**, ví dụ vào `/opt/imagerec/`:
   `scp server/target/server-1.0-SNAPSHOT-all.jar user@<ip>:/opt/imagerec/`
3. **Mô hình** YOLOv8s (ONNX) đã nằm trong jar nếu bạn tải nó trước khi build (`scripts/download-model.sh`, xem ADR-0003), nên VPS không phải tải gì lúc chạy. Có thể bỏ qua `--model`; chỉ dùng khi muốn nạp file ONNX khác (cần `synset.txt` cạnh nó).
4. **Mở cổng 5000**: trên VPS `sudo ufw allow 5000/tcp` (đồng thời `sudo ufw allow OpenSSH` trước khi bật ufw); và mở TCP 5000 trong firewall/security group của nhà cung cấp.
5. **Tên miền**: tạo bản ghi DNS **A** `<tên miền>` trỏ về IP tĩnh của VPS.
6. **Chạy bằng systemd**: tạo `/etc/systemd/system/imagerec.service`:

```ini
[Unit]
Description=Image Recognition Server
After=network-online.target
Wants=network-online.target

[Service]
User=imagerec
WorkingDirectory=/opt/imagerec
ExecStart=/usr/bin/java -jar /opt/imagerec/server-1.0-SNAPSHOT-all.jar --gui=false --detector=djl --port=5000 --maxClients=20
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Tạo user chạy dịch vụ (`sudo useradd -r -m -d /opt/imagerec imagerec`), rồi:

```
sudo systemctl daemon-reload
sudo systemctl enable --now imagerec
sudo systemctl status imagerec
```

7. **Xem log**: file `logs/server.log` trong thư mục làm việc (`/opt/imagerec/logs/server.log`), hoặc `journalctl -u imagerec -f`.

### Cấu hình đang triển khai (thực tế)

| Hạng mục | Giá trị |
|---|---|
| Nhà cung cấp | thuevpsgiare.vn, gói VPS SSD C5: 4 vCPU, 6 GB RAM, 60 GB NVMe |
| Hệ điều hành | Ubuntu 24.04 x86_64, OpenJDK 17.0.20 |
| IP | 103.245.237.199 (IPv4 tĩnh, gắn trực tiếp trên eth0, không NAT) |
| Thời hạn thuê | 16/10/2026 đến 16/11/2026 |
| Tên miền | `ai.devlinh.com`, bản ghi A, **DNS only** |
| SSH | user `linh`, chỉ dùng khóa ed25519; đã tắt đăng nhập root và mật khẩu (`/etc/ssh/sshd_config.d/00-hardening.conf`) |
| ufw | mở OpenSSH, 80, 443, 3000, 5000 (IPv4 và IPv6) |
| Dịch vụ | `imagerec.service`, user `imagerec`, thư mục `/opt/imagerec`, cổng 5000 |

Lưu ý:

- Bản ghi `ai` phải để **DNS only** (mây xám) vì Cloudflare gói miễn phí không chuyển tiếp TCP cổng 5000. Các bản ghi web (`@`, `www`, `chat`) dùng Proxied bình thường.
- VPS còn chạy nginx, certbot, Node (server chat cổng 3000), nên RAM phải chia sẻ. Bộ nhớ native của engine suy luận (MXNet trước đây, nay là ONNX Runtime) nằm ngoài heap Java, vì vậy dịch vụ dùng `-Xmx2g` (ban đầu là `-Xmx4g`). Đã tạo swap 2 GB (17/10/2026) bằng:
  `sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile && echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab`
- Mô hình nằm trong jar nên Server không tải gì khi khởi động; log in `Server listening on port 5000`.
- `sudo` của user `linh` cần mật khẩu; chạy lệnh sudo trong một terminal SSH có TTY. Muốn `linh` đọc `journalctl` không cần sudo thì thêm vào nhóm `systemd-journal` (chưa làm).

Số liệu đo ngày 17/10/2026 (client ở Windows, gọi qua Internet tới `ai.devlinh.com:5000`, 4 ảnh mẫu × 2 lượt):

| Chỉ số | Giá trị |
|---|---|
| Kết nối (HELLO → HELLO_OK) | ~90 ms |
| Xử lý trên Server một ảnh | 125–180 ms |
| Khứ hồi một ảnh (gửi → nhận kết quả) | 220–290 ms |
| RAM tiến trình Java sau 10 ảnh | RSS 423 MB, systemd Memory 331 MB; swap chưa dùng |

Ví dụ `dog_bike_car.jpg` trả về car 0,99; dog 0,95; dog 0,60; bicycle 0,34. Số RAM mới đo trong thời gian ngắn với một client, chưa phải tải lâu dài nhiều client.

`/etc/systemd/system/imagerec.service` đang dùng:

```ini
[Unit]
Description=Image Recognition Server
After=network-online.target
Wants=network-online.target

[Service]
User=imagerec
WorkingDirectory=/opt/imagerec
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar /opt/imagerec/server-1.0-SNAPSHOT-all.jar --gui=false --detector=djl --port=5000 --maxClients=20 --ortThreads=2
Nice=-5
OOMScoreAdjust=-500
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Quy trình cập nhật jar mới: `scp` file vào `/opt/imagerec/`, rồi `sudo systemctl restart imagerec` và xem `sudo journalctl -u imagerec -f`. Jar đang chạy là bản YOLOv8s 81 lớp kèm MonkeyGuard (mục 7); cách quay lại bản cũ ở cuối mục 7.

Bảo mật và dọn dẹp:

- Giao thức không có xác thực, ai biết địa chỉ cũng gửi được ảnh; `--maxClients=20` là giới hạn duy nhất. Sau buổi bảo vệ nên chạy `sudo ufw delete allow 5000/tcp`.
- Trước 16/11/2026 chép về máy: jar server, `imagerec.service`, `logs/server.log` cần cho báo cáo.
- Không đưa khóa SSH, mật khẩu hay token vào repo.

## 4. Phương án dự phòng: máy cá nhân + đường hầm

Chạy Server trên máy cá nhân rồi dùng ngrok (TCP) hoặc playit.gg để mở ra Internet, ví dụ `ngrok tcp 5000`. Dịch vụ cấp một địa chỉ và cổng (có thể đổi mỗi lần chạy), Client nhập đúng địa chỉ và cổng đó. Chỉ nên dùng khi chưa có VPS.

## 5. Kiểm tra từ Internet

Từ một mạng khác (ví dụ 4G), trong PowerShell:

```powershell
Test-NetConnection <tên miền> -Port 5000
```

Kết quả `TcpTestSucceeded : True` nghĩa là cổng đã mở. Sau đó mở Client, nhập tên miền và cổng, kết nối và nhận dạng một ảnh.

Nếu `False`: kiểm tra Server đang chạy (`systemctl status`), `ufw status`, firewall của nhà cung cấp, và bản ghi DNS đã trỏ đúng IP.

## 6. Chốt phương án

Đã chốt phương án **VPS** (thuê từ 16/10/2026). Đường hầm ở mục 4 chỉ còn là dự phòng nếu VPS gặp sự cố.

## 7. Mô hình đang chạy trên VPS, số đo và cách quay lại

Từ 22/10/2026 VPS chạy jar YOLOv8s 81 lớp (COCO 80 lớp + `monkey`, ADR-0003 và `docs/huan-luyen/nhat-ky.md`) kèm lớp bảo vệ MonkeyGuard, `ExecStart` có `--ortThreads=2`:

```
/usr/bin/java -Xms512m -Xmx2g -jar /opt/imagerec/server-1.0-SNAPSHOT-all.jar --gui=false --detector=djl --port=5000 --maxClients=20 --ortThreads=2
```

MonkeyGuard là luật hậu xử lý sau NMS: bỏ khung `monkey` có độ tin cậy dưới 0,5; nếu còn khung `monkey` thì chạy thêm YOLOv8s gốc 80 lớp (nạp một lần lúc khởi động) và bỏ khung `monkey` trùng (IoU từ 0,5) với khung `person` có độ tin cậy từ 0,2. Ảnh không có khỉ không tốn thêm một lần suy luận. **Mã MonkeyGuard thuộc issue #15 (GitHub #34), chưa có trong nhánh chứa tài liệu này**; số đo bên dưới là của jar đang chạy trên VPS.

`--ortThreads=N` (hoặc `ortThreads=N` trong `server.properties`) đặt số luồng intra-op của ONNX Runtime; để trống thì dùng mặc định (số lõi). Thử trên VPS (4 vCPU) bằng một Server phụ ở cổng 5001, đo qua loopback, 4 ảnh mẫu, 3 lượt sau một lượt khởi động (dịch vụ chính vẫn chạy song song nên có nhiễu):

| `--ortThreads` | 1 Client: Server trung vị (ms) | 5 Client: Server trung vị (ms) | 5 Client: tổng 40 yêu cầu (s) |
|---|---|---|---|
| mặc định | 321-345 | 1534-1594 | 14,2-14,6 |
| 2 | 329-386 | 1224-1340 | 11,7-12,2 |

`--ortThreads=2` làm 5 Client nhanh hơn khoảng 15-20%, 1 Client không đổi rõ.

Số đo qua Internet với jar đang chạy (Client ở Windows gọi `ai.devlinh.com:5000`, mỗi Client 2 lượt x 4 ảnh, 5 Client chờ nhau rồi cùng gửi, 3 lần chạy mỗi dòng):

| Trường hợp | Khứ hồi trung vị / p90 (ms) | Server xử lý trung vị (ms) |
|---|---|---|
| 1 Client, 4 ảnh không có khỉ | 452-515 / 502-749 | 341-403 |
| 5 Client, 4 ảnh không có khỉ | 1391-1531 / 1707-1848 | 1198-1347 |
| 1 Client, `monkey.jpg` (4 lượt) | 711-827 | 628-747 |
| 5 Client, `monkey.jpg` (2 lượt, 1 lần chạy) | 2609 / 2899 | 2482 |

- Ảnh có khỉ chậm hơn ảnh không có khỉ khoảng 250-350 ms vì chạy thêm mô hình của MonkeyGuard. Dòng 5 Client với `monkey.jpg` chỉ chạy một lần, mang tính tham khảo.
- RAM: tiến trình Java RSS khoảng 1,47 GB sau các đợt đo (trước khi có MonkeyGuard khoảng 1,0 GB); `free -m` còn available khoảng 3,9 GB. Chưa tách được nguyên nhân mức tăng.
- Kết nối (HELLO đến HELLO_OK): khoảng 60-90 ms.
- Số đo không cô lập (mạng và VPS dùng chung), chưa có khoảng tin cậy.

### Các bản sao lưu jar và cách quay lại

Cạnh jar trong `/opt/imagerec/` có các bản sao lưu theo từng lần nâng cấp:

| File | Nội dung |
|---|---|
| `server-1.0-SNAPSHOT-all.jar.bak-ssd` | SSD-MobileNet (bản đầu) |
| `server-1.0-SNAPSHOT-all.jar.bak-yolo80` | YOLOv8s gốc 80 lớp |
| `server-1.0-SNAPSHOT-all.jar.bak-coco81` | YOLOv8s 81 lớp, chưa có MonkeyGuard |

Quay lại một bản (cần `sudo`, chạy trong terminal SSH có TTY):

```
cd /opt/imagerec
sudo cp server-1.0-SNAPSHOT-all.jar.bak-coco81 server-1.0-SNAPSHOT-all.jar
sudo chown imagerec: server-1.0-SNAPSHOT-all.jar
sudo systemctl restart imagerec
systemctl status imagerec --no-pager
```

Hai bản `.bak-ssd` và `.bak-yolo80` không có tham số `--ortThreads`: nếu quay về hai bản này thì xóa `--ortThreads=2` khỏi `ExecStart`, rồi `sudo systemctl daemon-reload` trước khi restart.

## 8. Đóng gói cho Windows

Ba script trong `scripts/` (chạy tại thư mục `ma-nguon`, cần JDK 21 trở lên có `jpackage`; chỉ chứa ký tự ASCII để Windows PowerShell 5.1 đọc đúng). Kết quả nằm trong `dist/`, thư mục này đã bị `.gitignore` chặn nên không được commit.

1. Tải mô hình trước (script Server kiểm tra và báo lỗi nếu thiếu): `scripts\download-model.cmd`.
2. `powershell -ExecutionPolicy Bypass -File scripts\package-client.ps1` tạo `dist\NhanDangAnh\NhanDangAnh.exe` (kèm Java thu gọn chỉ gồm `java.base` và `java.desktop`) và `dist\NhanDangAnh-windows.zip` (khoảng 31 MB).
3. `powershell -ExecutionPolicy Bypass -File scripts\package-server.ps1` tạo `dist\NhanDangAnh-Server\NhanDangAnh-Server.exe` (khoảng 136 MB), mặc định mở cửa sổ Server với `--detector=djl --port=5000`; có thể thêm tham số khi chạy, ví dụ `NhanDangAnh-Server.exe --gui=false --port=5055`. Script chép `msvcp140.dll`, `vcruntime140.dll`, `vcruntime140_1.dll` từ `C:\Windows\System32` đè lên bản cũ trong `runtime\bin`; JDK của Oracle kèm bản cũ làm ONNX Runtime báo `UnsatisfiedLinkError` (xem ADR-0003).
4. `powershell -ExecutionPolicy Bypass -File scripts\package-release.ps1 -Guide <HUONG-DAN-SU-DUNG.md> -DocsDir <thư mục chứa NHOM01-*.docx, .pdf, .pptx>` tạo `dist\NhanDangAnh-Nhom01.zip` (khoảng 250 MB) gồm `chuong-trinh/` (hai app-image và hai jar), `ma-nguon/` (lấy bằng `git archive HEAD`, chỉ gồm file đã commit), `tai-lieu/` và `HUONG-DAN-SU-DUNG.md`. Hướng dẫn sử dụng và báo cáo, slide không nằm trong repo nên hai tham số này bắt buộc; thiếu file nào thì script dừng và báo tên file đó.
