package com.jhuangyc.cgmap.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.jhuangyc.cgmap.io.GraphicFileReader
import com.jhuangyc.cgmap.io.GraphicInfoFileReader
import com.jhuangyc.cgmap.io.PaletFileReader
import com.jhuangyc.cgmap.painter.GraphicPainter
import com.jhuangyc.cgmap.util.dimension
import com.jhuangyc.cgmap.util.saveTo
import com.jhuangyc.cgmap.util.toHexTable
import mu.KotlinLogging
import java.awt.Dimension
import java.awt.Graphics
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JFrame
import javax.swing.JPanel

/**
 * Example usage:
 *
 */
object GraphicCommand : CliktCommand(
	name = "graphic",
	help = "Show info for a GraphicCommand entity"
) {
	private val logger = KotlinLogging.logger {}

	//region CLI arguments and flags
	private val graphicNo: Int by argument(
		name = "<graphic_no>",
		help = "The ordinal number of the graphic in the GraphicInfo file")
		.int()

	private val print: Boolean by option(
		"-d",
		help = "Show the content of GraphicCommand data in hex")
		.flag()

	private val paint: Boolean by option(
		"-p",
		help = "Paint the GraphicCommand to screen or file")
		.flag()

	private val outputFile: Path? by option(
		"-o",
		help = "The file to save the painted GraphicCommand. GUI if not specified.")
		.path(folderOkay = false)

	private val graphicInfoFile: Path by option(
		"--graphic_info_file",
		help = "The GraphicInfo file (.bin)")
		.path(exists = true, folderOkay = false)
		.default(Paths.get("data", "GraphicInfo_66.bin"))

	private val graphicFile: Path by option(
		"--graphic_file",
		help = "The GraphicCommand file (.bin)")
		.path(exists = true, folderOkay = false)
		.default(Paths.get("data", "Graphic_66.bin"))

	private val paletFile: Path by option(
		"--palet_file",
		help = "The Palet file (.cgp) to use for painting the graphic")
		.path(exists = true, folderOkay = false)
		.default(Paths.get("data", "palet", "palet_00.cgp"))
	//endregion

	override fun run() {
		logger.info("Run graphic command")
		logger.info(
			"Displaying GraphicCommand #${graphicNo} from ${graphicInfoFile}...")

		//region Reading and displaying GraphicInfo
		val graphicInfo = GraphicInfoFileReader(graphicInfoFile).use {
			it.read(graphicNo)
		}
		echo(graphicInfo.toString().replace(", ", ",\n  "))
		echo("")
		//endregion

		//region Reading and displaying GraphicCommand metadata
		echo("Displaying GraphicCommand #${graphicNo} from ${graphicFile}...")
		val graphic = GraphicFileReader(graphicFile).use {
			it.read(graphicInfo.address, graphicInfo.dataLength)
		}
		echo(graphic.toString().replace(", ", ",\n  "))
		echo("")

		if (print) {
			logger.info("Printing GraphicCommand data as hex")
			echo(graphic.data.toHexTable())
			echo("")
		}
		//endregion

		//region Painting GraphicCommand
		if (paint) {
			val palet = PaletFileReader(paletFile).read()
			val paintedGraphic = GraphicPainter(graphicInfo, graphic, palet).paint()

			logger.info("Painting GraphicCommand to file: ${outputFile}")
			if (outputFile == null) {
				//region To screen
				val frame = JFrame("MainCommand GraphicCommand")
				frame.add(object : JPanel() {
					override fun paint(g: Graphics?) {
						g!!.drawImage(paintedGraphic, 0, 0, null)
					}

					override fun getPreferredSize(): Dimension {
						return paintedGraphic.dimension()
					}
				})
				frame.pack()
				frame.isVisible = true
				frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
				//endregion

			} else {
				//region To file
				echo("Painting GraphicCommand to file ${outputFile}")
				paintedGraphic.saveTo(outputFile!!)
				//endregion
			}
		}
		//endregion
	}
}

