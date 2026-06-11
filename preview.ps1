param(
    [ValidateSet('doctor', 'devices', 'emulators', 'start-emulator', 'build', 'install', 'start', 'run', 'logcat', 'startup-log', 'help')]
    [string]$Action = 'run',

    [ValidateSet('debug', 'release')]
    [string]$Variant = 'debug',

    [string]$DeviceId,

    [string]$AvdName,

    [string]$PackageName = 'com.example.gym_app',

    [string]$ActivityName = '.MainActivity'
)

$ErrorActionPreference = 'Stop'

function Write-Step {
    param([string]$Message)
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Get-ProjectRoot {
    Split-Path -Parent $PSCommandPath
}

function Get-LocalPropertyValue {
    param(
        [string]$ProjectRoot,
        [string]$Key
    )

    $localProperties = Join-Path $ProjectRoot 'local.properties'
    if (-not (Test-Path $localProperties)) {
        return $null
    }

    foreach ($line in Get-Content $localProperties) {
        if ($line -match "^$([regex]::Escape($Key))=(.*)$") {
            return $Matches[1]
        }
    }

    return $null
}

function Get-AndroidSdkPath {
    param([string]$ProjectRoot)

    if ($env:ANDROID_SDK_ROOT) {
        return $env:ANDROID_SDK_ROOT
    }

    if ($env:ANDROID_HOME) {
        return $env:ANDROID_HOME
    }

    $rawSdkDir = Get-LocalPropertyValue -ProjectRoot $ProjectRoot -Key 'sdk.dir'
    if ($rawSdkDir) {
        return ($rawSdkDir -replace '\\:', ':') -replace '\\\\', '\'
    }

    return $null
}

function Resolve-ToolPath {
    param(
        [string]$CommandName,
        [string]$SdkPath,
        [string[]]$Candidates
    )

    $command = Get-Command $CommandName -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    if ($SdkPath) {
        foreach ($candidate in $Candidates) {
            $fullPath = Join-Path $SdkPath $candidate
            if (Test-Path $fullPath) {
                return $fullPath
            }
        }
    }

    throw "Tool not found: $CommandName. Install Android SDK platform-tools or add it to PATH."
}

function Resolve-OptionalToolPath {
    param(
        [string]$CommandName,
        [string]$SdkPath,
        [string[]]$Candidates
    )

    $command = Get-Command $CommandName -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    if ($SdkPath) {
        foreach ($candidate in $Candidates) {
            $fullPath = Join-Path $SdkPath $candidate
            if (Test-Path $fullPath) {
                return $fullPath
            }
        }
    }

    return $null
}

function Get-AdbArgumentsPrefix {
    if ([string]::IsNullOrWhiteSpace($DeviceId)) {
        return @()
    }

    return @('-s', $DeviceId)
}

function Invoke-Adb {
    param(
        [string]$AdbPath,
        [string[]]$Arguments,
        [switch]$Passthrough
    )

    $prefix = Get-AdbArgumentsPrefix
    $allArgs = $prefix + $Arguments

    if ($Passthrough) {
        & $AdbPath @allArgs
        return
    }

    & $AdbPath @allArgs
}

function Get-ConnectedDevices {
    param([string]$AdbPath)

    $output = Invoke-Adb -AdbPath $AdbPath -Arguments @('devices')
    $devices = @()

    foreach ($line in $output) {
        if ($line -match '^(?<id>[^\s]+)\s+device$') {
            $devices += $Matches['id']
        }
    }

    return @($devices)
}

function Wait-ForDevice {
    param(
        [string]$AdbPath,
        [int]$TimeoutSeconds = 180
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $devices = Get-ConnectedDevices -AdbPath $AdbPath
        if ($DeviceId) {
            if ($devices -contains $DeviceId) {
                return $DeviceId
            }
        } elseif ($devices.Count -eq 1) {
            return $devices[0]
        } elseif ($devices.Count -gt 1) {
            throw 'Multiple devices detected. Use -DeviceId to select one.'
        }

        Start-Sleep -Seconds 3
    } while ((Get-Date) -lt $deadline)

    if ($DeviceId) {
        throw "Timed out waiting for device: $DeviceId"
    }

    throw 'Timed out waiting for an Android device or emulator to become available.'
}

function Ensure-DeviceAvailableOrStartEmulator {
    param(
        [string]$AdbPath,
        [string]$EmulatorPath
    )

    $devices = Get-ConnectedDevices -AdbPath $AdbPath
    if ($DeviceId) {
        if ($devices -contains $DeviceId) {
            return
        }
        throw "Device not found: $DeviceId. Run ./preview.ps1 devices first."
    }

    if ($devices.Count -eq 1) {
        return
    }

    if ($devices.Count -gt 1) {
        throw 'Multiple devices detected. Use -DeviceId to select one.'
    }

    if (-not $EmulatorPath) {
        throw 'No connected device found, and Android emulator tool is unavailable. Install the emulator package or connect a device.'
    }

    $avds = Get-AvailableAvds -EmulatorPath $EmulatorPath
    if ($avds.Count -eq 0) {
        throw 'No connected device found, and no AVD is available. Create an emulator in Android Studio Device Manager first.'
    }

    $targetAvd = if ([string]::IsNullOrWhiteSpace($AvdName)) {
        if ($avds -contains 'Pixel_8') { 'Pixel_8' } else { $avds[0] }
    } else {
        $AvdName
    }
    if ($avds -notcontains $targetAvd) {
        throw "AVD not found: $targetAvd. Run ./preview.bat emulators first."
    }

    Start-Emulator -EmulatorPath $EmulatorPath
    Write-Step 'Waiting for emulator to connect to adb'
    $connectedDevice = Wait-ForDevice -AdbPath $AdbPath
    Write-Step "Device ready: $connectedDevice"
}

function Ensure-SingleDeviceOrSelected {
    param([string]$AdbPath)

    $devices = Get-ConnectedDevices -AdbPath $AdbPath
    if ($DeviceId) {
        if ($devices -notcontains $DeviceId) {
            throw "Device not found: $DeviceId. Run ./preview.ps1 devices first."
        }
        return
    }

    if ($devices.Count -eq 0) {
        throw 'No connected device found. Connect a phone, or run ./preview.bat start-emulator -AvdName Pixel_8.'
    }

    if ($devices.Count -gt 1) {
        throw 'Multiple devices detected. Use -DeviceId to select one.'
    }
}

function Get-AvailableAvds {
    param([string]$EmulatorPath)

    if (-not $EmulatorPath) {
        throw 'Android emulator tool not found. Install the emulator package in Android SDK.'
    }

    $output = & $EmulatorPath -list-avds
    return @($output | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}

function Show-Emulators {
    param([string]$EmulatorPath)

    Write-Step 'Available emulators'
    $avds = Get-AvailableAvds -EmulatorPath $EmulatorPath
    if ($avds.Count -eq 0) {
        Write-Host 'No AVD found.'
        return
    }

    $avds | ForEach-Object { Write-Host $_ }
}

function Start-Emulator {
    param([string]$EmulatorPath)

    $avds = Get-AvailableAvds -EmulatorPath $EmulatorPath
    if ($avds.Count -eq 0) {
        throw 'No AVD found. Create one first in Android Studio Device Manager.'
    }

    $targetAvd = $AvdName
    if ([string]::IsNullOrWhiteSpace($targetAvd)) {
        $targetAvd = if ($avds -contains 'Pixel_8') { 'Pixel_8' } else { $avds[0] }
    }

    if ($avds -notcontains $targetAvd) {
        throw "AVD not found: $targetAvd. Run ./preview.bat emulators first."
    }

    Write-Step "Starting emulator $targetAvd"
    Start-Process -FilePath $EmulatorPath -ArgumentList '-avd', $targetAvd -WindowStyle Hidden | Out-Null
}

function Get-ApkPath {
    param(
        [string]$ProjectRoot,
        [string]$VariantName
    )

    $apkName = if ($VariantName -eq 'release') { 'app-release.apk' } else { 'app-debug.apk' }
    Join-Path $ProjectRoot "app\build\outputs\apk\$VariantName\$apkName"
}

function Build-Apk {
    param(
        [string]$ProjectRoot,
        [string]$VariantName
    )

    $taskName = if ($VariantName -eq 'release') { 'assembleRelease' } else { 'assembleDebug' }
    Write-Step "Building APK with Gradle task $taskName"
    & (Join-Path $ProjectRoot 'gradlew.bat') $taskName
}

function Install-Apk {
    param(
        [string]$AdbPath,
        [string]$ApkPath
    )

    if (-not (Test-Path $ApkPath)) {
        throw "APK not found: $ApkPath. Run build first."
    }

    Ensure-SingleDeviceOrSelected -AdbPath $AdbPath
    Write-Step "Installing APK $ApkPath"
    Invoke-Adb -AdbPath $AdbPath -Arguments @('install', '-r', $ApkPath) -Passthrough
}

function Start-App {
    param(
        [string]$AdbPath,
        [string]$Package,
        [string]$Activity
    )

    Ensure-SingleDeviceOrSelected -AdbPath $AdbPath
    Write-Step "Starting $Package/$Activity"
    Invoke-Adb -AdbPath $AdbPath -Arguments @('shell', 'am', 'start', '-n', "$Package/$Activity") -Passthrough
}

function Get-AppPid {
    param(
        [string]$AdbPath,
        [string]$Package
    )

    $pidOutput = Invoke-Adb -AdbPath $AdbPath -Arguments @('shell', 'pidof', $Package)
    if (-not $pidOutput) {
        return $null
    }

    $pidText = ($pidOutput | Select-Object -First 1).Trim()
    if ([string]::IsNullOrWhiteSpace($pidText)) {
        return $null
    }

    return ($pidText -split '\s+')[0]
}

function Show-Devices {
    param([string]$AdbPath)

    Write-Step 'Connected devices'
    Invoke-Adb -AdbPath $AdbPath -Arguments @('devices') -Passthrough
}

function Show-Logcat {
    param(
        [string]$AdbPath,
        [string]$Package
    )

    Ensure-SingleDeviceOrSelected -AdbPath $AdbPath
    $pid = Get-AppPid -AdbPath $AdbPath -Package $Package

    if ($pid) {
        Write-Step "Streaming logcat for PID $pid"
        Invoke-Adb -AdbPath $AdbPath -Arguments @('logcat', '--pid', $pid) -Passthrough
        return
    }

    Write-Step 'PID not available yet, falling back to full logcat'
    Invoke-Adb -AdbPath $AdbPath -Arguments @('logcat') -Passthrough
}

function Show-StartupLog {
    param(
        [string]$AdbPath,
        [string]$Package,
        [string]$Activity
    )

    Ensure-SingleDeviceOrSelected -AdbPath $AdbPath

    Write-Step 'Clearing old logcat buffer'
    Invoke-Adb -AdbPath $AdbPath -Arguments @('logcat', '-c') -Passthrough

    Start-App -AdbPath $AdbPath -Package $Package -Activity $Activity
    Start-Sleep -Seconds 2

    $pid = Get-AppPid -AdbPath $AdbPath -Package $Package
    if ($pid) {
        Write-Step "Streaming startup log for PID $pid"
        Invoke-Adb -AdbPath $AdbPath -Arguments @('logcat', '--pid', $pid) -Passthrough
        return
    }

    Write-Step 'PID not available yet, falling back to full logcat'
    Invoke-Adb -AdbPath $AdbPath -Arguments @('logcat') -Passthrough
}

function Show-Help {
    @'
Usage:
  ./preview.ps1 [action] [-Variant debug|release] [-DeviceId device-id]

Actions:
  doctor       Check SDK, adb, Gradle, and connected devices
  devices      List connected devices
  emulators    List available Android emulators
  start-emulator  Start an Android emulator
  build        Build APK
  install      Install APK
  start        Start app
  run          Build + install + start
  logcat       Stream logs
  startup-log  Clear logs, start app, then stream startup logs
  help         Show help

Examples:
  ./preview.ps1 doctor
  ./preview.ps1 emulators
  ./preview.ps1 start-emulator -AvdName Pixel_8
  ./preview.ps1 run
  ./preview.ps1 run -DeviceId emulator-5554
  ./preview.ps1 startup-log
'@ | Write-Host
}

$projectRoot = Get-ProjectRoot
$sdkPath = Get-AndroidSdkPath -ProjectRoot $projectRoot
$adbPath = Resolve-ToolPath -CommandName 'adb' -SdkPath $sdkPath -Candidates @(
    'platform-tools\adb.exe',
    'platform-tools\adb'
)
$emulatorPath = Resolve-OptionalToolPath -CommandName 'emulator' -SdkPath $sdkPath -Candidates @(
    'emulator\emulator.exe',
    'emulator\emulator'
)

switch ($Action) {
    'doctor' {
        Write-Step 'Environment check'
        Write-Host "Project root: $projectRoot"
        Write-Host "Android SDK: $sdkPath"
        Write-Host "adb path: $adbPath"
        Write-Host "Gradle wrapper: $(Join-Path $projectRoot 'gradlew.bat')"
        Show-Devices -AdbPath $adbPath
    }
    'devices' {
        Show-Devices -AdbPath $adbPath
    }
    'emulators' {
        Show-Emulators -EmulatorPath $emulatorPath
    }
    'start-emulator' {
        Start-Emulator -EmulatorPath $emulatorPath
    }
    'build' {
        Build-Apk -ProjectRoot $projectRoot -VariantName $Variant
        Write-Step "Build finished: $(Get-ApkPath -ProjectRoot $projectRoot -VariantName $Variant)"
    }
    'install' {
        Ensure-DeviceAvailableOrStartEmulator -AdbPath $adbPath -EmulatorPath $emulatorPath
        Install-Apk -AdbPath $adbPath -ApkPath (Get-ApkPath -ProjectRoot $projectRoot -VariantName $Variant)
    }
    'start' {
        Ensure-DeviceAvailableOrStartEmulator -AdbPath $adbPath -EmulatorPath $emulatorPath
        Start-App -AdbPath $adbPath -Package $PackageName -Activity $ActivityName
    }
    'run' {
        Build-Apk -ProjectRoot $projectRoot -VariantName $Variant
        Ensure-DeviceAvailableOrStartEmulator -AdbPath $adbPath -EmulatorPath $emulatorPath
        Install-Apk -AdbPath $adbPath -ApkPath (Get-ApkPath -ProjectRoot $projectRoot -VariantName $Variant)
        Start-App -AdbPath $adbPath -Package $PackageName -Activity $ActivityName
    }
    'logcat' {
        Ensure-DeviceAvailableOrStartEmulator -AdbPath $adbPath -EmulatorPath $emulatorPath
        Show-Logcat -AdbPath $adbPath -Package $PackageName
    }
    'startup-log' {
        Ensure-DeviceAvailableOrStartEmulator -AdbPath $adbPath -EmulatorPath $emulatorPath
        Show-StartupLog -AdbPath $adbPath -Package $PackageName -Activity $ActivityName
    }
    'help' {
        Show-Help
    }
    default {
        Show-Help
    }
}
