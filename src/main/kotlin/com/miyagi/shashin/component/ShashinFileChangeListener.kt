package com.miyagi.shashin.component

import com.miyagi.shashin.controller.SettingsController
import org.springframework.boot.devtools.filewatch.ChangedFile
import org.springframework.boot.devtools.filewatch.ChangedFiles
import org.springframework.boot.devtools.filewatch.FileChangeListener
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import java.util.logging.Level
import java.util.logging.Logger

@Component
class ShashinFileChangeListener(val settingsController: SettingsController): FileChangeListener {

    private var logger: Logger = Logger.getLogger(ShashinFileChangeListener::class.simpleName)

    @Override
    override fun onChange(changeSet: Set<ChangedFiles>) {
        var changeDetected = false
        for (cfiles: ChangedFiles in changeSet) {
            for (cfile: ChangedFile in cfiles.files) {
                if (cfile.type.equals(ChangedFile.Type.MODIFY)
                 || cfile.type.equals(ChangedFile.Type.ADD)
                 || cfile.type.equals(ChangedFile.Type.DELETE)
                ) {
                    logger.log(Level.INFO, "ShashinFileChangeListener operation: " + cfile.type + " On file: "+ cfile.file.name + " is done")
                    changeDetected = true
                    break
                }
                if (changeDetected) {
                    break
                }
            }
        }

        if (changeDetected) {
            val reindex = false
            val msg = settingsController.scanMediaDirectories(reindex, 0, 0, LocaleContextHolder.getLocale())
            logger.log(Level.INFO, "ShashinFileChangeListener scanMediaDirectories msg: $msg")
        }
    }
}