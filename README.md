# PolarAlerts

A simple **SPIGOT** plugin that sends Polar anticheat alerts across your BungeeCord network via plugin messaging.

## What it does

- Forwards alerts from Polar to all your servers
- Staff can see alerts from any server on your network
- Works with regular BungeeCord - no extra plugins needed
- Supports all Polar alert types

## Setup

### Network Setup (Two Options)

#### Option 1: Alert Sending + Receiving
Put `PolarAlerts.jar` on:
- Servers with Polar installed (to send alerts)
- Any server where staff should see alerts

#### Option 2: Listen-Only Mode
For servers without Polar that just need to display alerts:
1. Put `PolarAlerts.jar` in the plugins folder
2. Edit config.yml and set `listen-only-mode: true`

This mode doesn't require Polar to be installed.

## Configuration

### Basic config (works for most servers)

```yaml
# Listen-only mode for servers without Polar
listen-only-mode: false  # Set to true on servers without Polar

# Server name that shows in alerts
server-name: 'survival'

# Display settings
display:
  enabled: true
  permission: "anticheat.alerts"
```

### Full config options

```yaml
# Listen-only mode (set to true for servers without Polar)
listen-only-mode: false

# Debug mode (enables extra logging)
debug: false

# Your server name (if left blank, will try to use Bukkit server ID)
server-name: 'survival'

# Which alert types to forward
alerts:
  detection: true
  cloud: true
  mitigation: true
  # How often to send mitigation alerts (1 = every alert, 5 = every 5th alert, etc.)
  mitigation-frequency: 1
  punishment: true

# Forwarding settings
forward:
  bungee:
    enabled: true
    forward-to-all: true
    # Target specific servers (only used if forward-to-all is false)
    targets:
      DETECTION:
        - 'staff'
      CLOUD:
        - 'staff'
      MITIGATION:
        - 'staff'
      PUNISHMENT:
        - 'ALL'  # Special value for all servers

# Display settings
display:
  enabled: true
  permission: "anticheat.alerts"
  
  # Which alert types to show
  alert-types:
    - "DETECTION"
    - "CLOUD"
    - "MITIGATION"
    - "PUNISHMENT"
  
  # Log alerts to console
  console-log: true
  
  # Make alerts clickable (clicking runs /server command)
  clickable: true
  
  # Show detailed information when hovering over alerts
  show-details-in-hover: true
  
  # Text shown when hovering over an alert
  hover-text: "§7Click to connect to §f%server%\n§7Player: §f%player%\n§7Check: §f%check%\n§7Type: §f%type%\n§7VL: §f%vl%"
  
  # Change how alerts look
  formats:
    detection: "§7[§b❀§7] §7[%server%] §f%player% failed §b%check% §fVL: %vl%"
    cloud: "§7[§b☁§7] §7[%server%] §f%player% failed §b%type% §7(%check%)"
    mitigation: "§7[§b❀§7] §7[%server%] §f%player% mitigated §b%check% §fVL: %vl%"
    punishment: "§7[§c⚠§7] §7[%server%] §f%player% was punished for §b%check%"
```

## Permissions

- `anticheat.alerts` - Lets staff see alerts

## Setup Examples

### Example 1: Survival network with lobby

On survival server (has Polar installed):
```yaml
listen-only-mode: false
server-name: 'survival'
```

On lobby server (doesn't have Polar):
```yaml
listen-only-mode: true
display:
  enabled: true
```

### Example 2: Staff-only alerts server

On all gameplay servers (with Polar):
```yaml
listen-only-mode: false
server-name: 'server1'  # Unique name for each server
forward:
  bungee:
    enabled: true
    forward-to-all: false
    targets:
      DETECTION:
        - 'staff'  # Only send to staff server
```

On staff server (without Polar):
```yaml
listen-only-mode: true
display:
  enabled: true
```

## Troubleshooting

- **No alerts showing up?** Make sure Polar is working on at least one server
- **Listen-only mode not working?** Check that at least one server is sending alerts
- **Staff can't see alerts?** Make sure they have the `anticheat.alerts` permission

## Support

Need help?
- Discord: [Link](https://polar.top/discord)
