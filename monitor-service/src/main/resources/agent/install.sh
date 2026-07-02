#!/usr/bin/env bash
#
# slowloris-agent 安装脚本
# 用法（由平台「获取探针安装命令」自动生成，请勿手动拼写）：
#   curl -fsSL https://平台/internal/agent/install.sh | sudo bash -s -- \
#        --server=https://平台 --target=本机监控IP --token=令牌 [--interval=30] [--web-ports=80,443]
#
# 在目标服务器上安装一个 systemd 常驻服务，定期采集本机【真实】网络指标
# （Web端口上的挂起连接 / 真实来源 IP / 唯一来源数 / 连接时长 / 包大小）回传平台用于攻击检测与溯源。
# --web-ports 不填则自动探测本机 LISTEN 端口（排除 SSH/22）。
set -euo pipefail

SERVER=""; TARGET=""; TOKEN=""; INTERVAL="30"; WEB_PORTS=""
for arg in "$@"; do
  case "$arg" in
    --server=*)    SERVER="${arg#*=}" ;;
    --target=*)    TARGET="${arg#*=}" ;;
    --token=*)     TOKEN="${arg#*=}" ;;
    --interval=*)  INTERVAL="${arg#*=}" ;;
    --web-ports=*) WEB_PORTS="${arg#*=}" ;;
    *) echo "未知参数: $arg" >&2 ;;
  esac
done

if [ -z "$SERVER" ] || [ -z "$TARGET" ] || [ -z "$TOKEN" ]; then
  echo "错误：缺少必需参数 --server / --target / --token" >&2
  exit 1
fi
if [ "$(id -u)" -ne 0 ]; then
  echo "错误：请用 root 运行（在命令前加 sudo）" >&2
  exit 1
fi
command -v ss   >/dev/null 2>&1 || { echo "缺少 ss（请先安装 iproute2）" >&2; exit 1; }
command -v curl >/dev/null 2>&1 || { echo "缺少 curl" >&2; exit 1; }

ENV_FILE="/etc/default/slowloris-agent"
BIN_FILE="/usr/local/bin/slowloris-agent.sh"
UNIT_FILE="/etc/systemd/system/slowloris-agent.service"

echo "[1/4] 写入配置 $ENV_FILE"
cat > "$ENV_FILE" <<EOF
SERVER=$SERVER
TARGET=$TARGET
TOKEN=$TOKEN
INTERVAL=$INTERVAL
WEB_PORTS=$WEB_PORTS
EOF
chmod 600 "$ENV_FILE"

# >>> AGENT_RUNTIME_PLACEHOLDER <<<

echo "[2/4] 写入采集器 $BIN_FILE"
cat > "$BIN_FILE" <<'AGENT_EOF'
#!/usr/bin/env bash
#
# slowloris-agent 采集器（常驻）。配置从 /etc/default/slowloris-agent 读取。
# 每 INTERVAL 秒采集一次本机真实网络指标并 POST 回平台。
set -o pipefail

# shellcheck disable=SC1091
. /etc/default/slowloris-agent
INTERVAL="${INTERVAL:-30}"
WEB_PORTS="${WEB_PORTS:-}"

REPORT_URL="${SERVER%/}/internal/agent/report"

# 确定要监控的 Web 服务端口：未指定则自动探测本机 LISTEN 端口（排除 SSH/22）。
# Slowloris 耗尽的是 Web 服务的连接池，因此必须盯住 Web 端口，而非全部连接。
detect_ports() {
  ss -tlnH 2>/dev/null \
    | awk '{print $4}' \
    | sed -E 's/.*:([0-9]+)$/\1/' \
    | grep -vE '^(22)$' \
    | sort -un | paste -sd, -
}

# 构造 ss 过滤表达式：本机作为服务端，监听端口即连接的本地源端口(sport)。
build_filter() {
  local ports="$1" expr="" p arr
  IFS=',' read -ra arr <<< "$ports"
  for p in "${arr[@]}"; do
    [ -z "$p" ] && continue
    [ -n "$expr" ] && expr="$expr or "
    expr="${expr}sport = :$p"
  done
  printf '%s' "$expr"
}

# /proc/net/dev 增量：用于估算平均包大小（总字节 / 总包数）
prev_bytes=0; prev_pkts=0
read_netdev() {
  # 汇总除 lo 外所有接口的 rx+tx 字节与包数
  awk -F'[: ]+' 'NR>2 && $2!="lo" {rb+=$3; rp+=$4; tb+=$11; tp+=$12}
                 END {print rb+tb, rp+tp}' /proc/net/dev
}

# 连接首次出现时间表（key=对端 addr:port），用于计算连接平均存活时长。
declare -A conn_first_seen=()

while true; do
  # 本轮要盯的 Web 端口（支持运行时自动探测 LISTEN 端口，排除 SSH）
  ports="${WEB_PORTS:-}"
  [ -z "$ports" ] && ports="$(detect_ports)"
  filter="$(build_filter "$ports")"

  now=$(date +%s)
  half_open=0
  # 每轮用普通变量替代关联数组做重置，避免 declare -A 在循环内重复声明的问题
  unset seen_now ip_count
  declare -A seen_now=()
  declare -A ip_count=()

  # 半开连接（应用层）：Slowloris 完成 TCP 握手后挂起，连接停在 ESTAB。
  # 因此核心特征是 Web 端口上大量长期 ESTAB 的连接，而非 TCP 层的 SYN-RECV。
  # 注意：ss -tanH state established 会省略 State 列，输出为 4 列：
  #   Recv-Q  Send-Q  Local  Peer
  # 用进程替换保证 while 在当前 shell 执行，关联数组得以跨周期保留。
  while read -r _rq _sq _local peer; do
    [ -z "$peer" ] && continue
    half_open=$((half_open + 1))
    seen_now["$peer"]=1
    [ -z "${conn_first_seen[$peer]:-}" ] && conn_first_seen[$peer]=$now
    # 对端(客户端)IP 才是真实来源——去端口，处理 IPv4-mapped IPv6（::ffff:x.x.x.x）
    # peer 格式可能是：1.2.3.4:port 或 [::ffff:1.2.3.4]:port 或 [::1]:port
    raw_ip=$(printf '%s' "$peer" | sed -E 's/:[0-9]+$//; s/^\[//; s/\]$//')
    # IPv4-mapped → 纯 IPv4
    pip=$(printf '%s' "$raw_ip" | sed -E 's/^::ffff://i')
    case "$pip" in 127.*|::1|0.0.0.0|\*|"") ;; *) ip_count["$pip"]=1 ;; esac
  done < <(if [ -n "$filter" ]; then ss -tanH state established "( $filter )" 2>/dev/null
           else ss -tanH state established 2>/dev/null; fi)

  # 平均连接时长 + 本周期新建连接数（基于首见时间表）
  total_dur=0; alive=0; new_conns=0
  for peer in "${!seen_now[@]}"; do
    fs=${conn_first_seen[$peer]:-$now}
    total_dur=$(( total_dur + now - fs ))
    alive=$((alive + 1))
    [ "$fs" -eq "$now" ] && new_conns=$((new_conns + 1))
  done
  # 清理已断开的连接记录，避免内存无限增长
  for peer in "${!conn_first_seen[@]}"; do
    [ -z "${seen_now[$peer]:-}" ] && unset "conn_first_seen[$peer]"
  done
  if [ "$alive" -gt 0 ]; then
    avg_dur=$(( total_dur * 1000 / alive ))   # 秒 → 毫秒
  else
    avg_dur=0
  fi

  unique_sources=${#ip_count[@]}
  # Top 20 真实来源 IP 作为溯源候选
  src_json=""
  if [ "${#ip_count[@]}" -gt 0 ]; then
    src_json=$(printf '%s\n' "${!ip_count[@]}" | head -20 \
        | awk 'NF{printf "%s\"%s\"", sep, $0; sep=","}')
  fi

  # 请求速率（次/分钟）：本周期新建连接数归一化。Slowloris 饱和后只挂连接、
  # 极少发起新连接，故速率偏低；正常 Web 流量不断开合连接，速率高。
  req_rate=$(( new_conns * 60 / (INTERVAL > 0 ? INTERVAL : 30) ))

  # 平均包大小：netdev 增量
  read cur_bytes cur_pkts < <(read_netdev)
  dpkts=$(( cur_pkts - prev_pkts ))
  dbytes=$(( cur_bytes - prev_bytes ))
  prev_bytes=$cur_bytes; prev_pkts=$cur_pkts
  if [ "$dpkts" -gt 0 ] && [ "$dbytes" -gt 0 ]; then
    avg_pkt=$(( dbytes / dpkts ))
  else
    avg_pkt=0
  fi
  [ "$avg_pkt" -gt 65535 ] && avg_pkt=65535

  payload="{\"target\":\"$TARGET\",\"token\":\"$TOKEN\",\"halfOpenConns\":$half_open,\"requestRate\":$req_rate,\"uniqueSourceIps\":$unique_sources,\"avgPacketSize\":$avg_pkt,\"avgConnDuration\":$avg_dur,\"sourceIps\":[$src_json]}"

  curl -fsS -m 10 -X POST "$REPORT_URL" \
       -H 'Content-Type: application/json' \
       -d "$payload" >/dev/null 2>&1 \
    || echo "$(date '+%F %T') 上报失败" >&2

  sleep "$INTERVAL"
done
AGENT_EOF
chmod 755 "$BIN_FILE"

echo "[3/4] 写入 systemd 服务 $UNIT_FILE"
cat > "$UNIT_FILE" <<EOF
[Unit]
Description=slowloris monitoring agent
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
EnvironmentFile=$ENV_FILE
ExecStart=$BIN_FILE
Restart=always
RestartSec=10
NoNewPrivileges=true

[Install]
WantedBy=multi-user.target
EOF

echo "[4/4] 启动服务"
systemctl daemon-reload
systemctl enable slowloris-agent.service >/dev/null 2>&1 || true
systemctl restart slowloris-agent.service

echo
echo "✅ slowloris-agent 已安装并启动。"
echo "   目标: $TARGET   平台: $SERVER   采集周期: ${INTERVAL}s"
echo "   监控端口: ${WEB_PORTS:-自动探测(排除SSH)}"
echo "   查看状态: systemctl status slowloris-agent"
echo "   查看日志: journalctl -u slowloris-agent -f"
echo "   卸载:     systemctl disable --now slowloris-agent && rm -f $BIN_FILE $UNIT_FILE $ENV_FILE && systemctl daemon-reload"
