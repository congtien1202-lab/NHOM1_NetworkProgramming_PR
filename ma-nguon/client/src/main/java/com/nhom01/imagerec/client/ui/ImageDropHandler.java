package com.nhom01.imagerec.client.ui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.TransferHandler;

/** Nhận file kéo thả từ Explorer; chỉ lấy file đầu tiên, việc kiểm tra định dạng do {@code onFile} lo. */
final class ImageDropHandler extends TransferHandler {

    private final BooleanSupplier enabled;
    private final Consumer<File> onFile;

    ImageDropHandler(BooleanSupplier enabled, Consumer<File> onFile) {
        this.enabled = enabled;
        this.onFile = onFile;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return enabled.getAsBoolean() && support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        try {
            List<?> files = (List<?>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            if (files.isEmpty() || !(files.get(0) instanceof File file)) {
                return false;
            }
            onFile.accept(file);
            return true;
        } catch (UnsupportedFlavorException | IOException e) {
            return false;
        }
    }
}
