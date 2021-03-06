package me.matrix4f.cardcutter.prefs.firstlaunch
import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import me.matrix4f.cardcutter.prefs.Prefs
import me.matrix4f.cardcutter.util.OS
import me.matrix4f.cardcutter.util.getOSType
import net.lingala.zip4j.ZipFile
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.PumpStreamHandler
import org.apache.commons.io.IOUtils
import org.apache.logging.log4j.LogManager
import java.awt.Desktop
import java.io.*
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Paths

private val logger = LogManager.getLogger(Prefs::class.java)

@Throws(FirstLaunchException::class, Exception::class)
private fun onFirstLaunchWindows() {
    val executable = File("CardifyChromeApp.exe")
    var dataStream = URL("http://cardifydebate.x10.bz/data/CardifyChromeAppWin.exe").openStream()
    var fos = FileOutputStream(executable)
    fos.channel.transferFrom(Channels.newChannel(dataStream), 0, Long.MAX_VALUE)
    dataStream.close()
    fos.close()

    val jsonFile = File("me.matrix4f.cardify.json")
    dataStream = URL("http://cardifydebate.x10.bz/data/ChromeAppNativeJsonWin.json").openStream()
    fos = FileOutputStream(jsonFile)
    fos.channel.transferFrom(Channels.newChannel(dataStream), 0, Long.MAX_VALUE)
    dataStream.close()
    fos.close()

    if (!jsonFile.exists())
        throw FirstLaunchException("Unable to download Chrome App Native JSON.")

    val commands = arrayOf(
        "REG DELETE \"HKCU\\Software\\Google\\Chrome\\NativeMessagingHosts\\me.matrix4f.cardify\" /f",
        "REG DELETE \"HKLM\\Software\\Google\\Chrome\\NativeMessagingHosts\\me.matrix4f.cardify\" /f",
        "REG ADD \"HKCU\\Software\\Google\\Chrome\\NativeMessagingHosts\\me.matrix4f.cardify\" /ve /t REG_SZ /d \"${jsonFile.absolutePath}\" /f"
    )
    for (cmd in commands) {
        val stdout = ByteArrayOutputStream()

        val stdoutPsh = PumpStreamHandler(stdout)
        val cmdLine = CommandLine.parse(cmd)
        val executor = DefaultExecutor()
        executor.streamHandler = stdoutPsh
        try {
            executor.execute(cmdLine)
        } catch (e: Exception) {
        }

        val result = stdout.toString().replace("\n", "")
        logger.info("Command '$cmd' returned '$result'")
    }
}

@Throws(FirstLaunchException::class, Exception::class)
private fun onFirstLaunchMacOS() {
    logger.info("Entered macOS First Launch")
    logger.info("Creating paths...")
    val jsonPath = Paths.get(System.getProperty("user.home"), "Library", "Application Support", "Google", "Chrome", "NativeMessagingHosts", "me.matrix4f.cardify.json")
    if (!jsonPath.parent.parent.toFile().exists())
        throw FirstLaunchException("No Google Chrome installation detected")

    val executablePath = Paths.get(System.getProperty("user.home"), "CardifyDebate", "CardifyChromeApp")
    val executableZipPath = Paths.get(System.getProperty("user.home"), "CardifyDebate", "CardifyChromeApp.zip")
    Files.createDirectories(executablePath.parent)

    logger.info("Opening data stream for json file...")
    var dataStream = URL("http://cardifydebate.x10.bz/data/ChromeAppNativeJsonMac.json").openStream()

    logger.info("Reading json file...")
    @Suppress("DEPRECATION") var data = IOUtils.toString(dataStream)
    IOUtils.closeQuietly(dataStream)

    logger.info("Writing json file to ${jsonPath.toFile().absolutePath} with executable ${executablePath.toFile().absolutePath}...")
    data = data.replace("%FILEPATH%", executablePath.toFile().absolutePath)
    Files.write(jsonPath, data.toByteArray())

    logger.info("Opening data stream for executable file...")
    dataStream = URL("http://cardifydebate.x10.bz/data/CardifyChromeAppMac.zip").openStream()
    logger.info("Downloading executable file...")
    val fos = FileOutputStream(executableZipPath.toFile())
    fos.channel.transferFrom(Channels.newChannel(dataStream), 0, Long.MAX_VALUE)
    dataStream.close()
    fos.close()

    logger.info("Extracting zipped executable...")
    val zipFile = ZipFile(executableZipPath.toFile())
    zipFile.extractAll(executablePath.parent.toFile().absolutePath)
    executableZipPath.toFile().deleteOnExit()

    val cmd = "chmod +x \"${executablePath.toFile().absolutePath}\""
    logger.info("Running command '$cmd'")
    val stdout = ByteArrayOutputStream()
    val stdoutPsh = PumpStreamHandler(stdout)
    val cmdLine = CommandLine.parse(cmd)
    val executor = DefaultExecutor()
    executor.streamHandler = stdoutPsh
    try {
        executor.execute(cmdLine)
    } catch (e: Exception) {
    }

    val result = stdout.toString().replace("\n", " ")
    logger.info("Command '$cmd' returned '$result'")

    if (!jsonPath.toFile().exists())
        throw FirstLaunchException("Unable to download Chrome App Native JSON.")
    if (!executablePath.toFile().exists())
        throw FirstLaunchException("Unable to download Chrome App Native Executable.")

}

fun onFirstLaunch(): Exception? {
    return try {
        logger.info("First launch method invoked with OS ${System.getProperty("os.name")}")
        if (getOSType() == OS.WINDOWS)
            onFirstLaunchWindows()
        if (getOSType() == OS.MAC)
            onFirstLaunchMacOS()
        null
    } catch (e: Exception) {
        e
    }
}
