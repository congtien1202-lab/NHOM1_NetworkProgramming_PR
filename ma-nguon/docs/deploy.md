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
3. **Tải sẵn mô hình** theo hướng dẫn trong `docs/adr/0002-chon-engine-djl.md` (và `scripts/download-model.*` nếu có) để Server không phải tải khi khởi động. Cache mặc định ở `~/.djl.ai`; có thể đặt biến môi trường `DJL_CACHE_DIR`. Chạy Server một lần để xác nhận lần chạy thứ hai không còn tải gì.
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
- VPS còn chạy nginx, certbot, Node (server chat cổng 3000), nên RAM phải chia sẻ. Bộ nhớ native của MXNet nằm ngoài heap Java, vì vậy dịch vụ dùng `-Xmx2g` (ban đầu là `-Xmx4g`). Đã tạo swap 2 GB (17/10/2026) bằng:
  `sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile && echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab`
- Mô hình `ssd_512_mobilenet1.0_voc` đã được cache; nạp khoảng 4,6 s, log in `Server listening on port 5000`.
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
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar /opt/imagerec/server-1.0-SNAPSHOT-all.jar --gui=false --detector=djl --port=5000 --maxClients=20
Nice=-5
OOMScoreAdjust=-500
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Quy trình cập nhật jar mới: `scp` file vào `/opt/imagerec/`, rồi `sudo systemctl restart imagerec` và xem `sudo journalctl -u imagerec -f`. Jar đang chạy là bản build ngày 11/10; mã nguồn chưa đổi từ đó nên chỉ cần chép lại khi sửa code.

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
