package com.nhom01.imagerec.client.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.TransferHandler.TransferSupport;
import org.junit.jupiter.api.Test;

class ImageDropHandlerTest {

    private final List<File> received = new ArrayList<>();

    @Test
    void passesFirstDroppedFile() {
        ImageDropHandler handler = new ImageDropHandler(() -> true, received::add);
        File first = new File("a.jpg");

        boolean imported = handler.importData(support(fileList(List.of(first, new File("b.png")))));

        assertThat(imported).isTrue();
        assertThat(received).containsExactly(first);
    }

    @Test
    void rejectsNonFileData() {
        ImageDropHandler handler = new ImageDropHandler(() -> true, received::add);

        assertThat(handler.importData(support(new StringSelection("text")))).isFalse();
        assertThat(received).isEmpty();
    }

    @Test
    void rejectsDropWhileDisabled() {
        ImageDropHandler handler = new ImageDropHandler(() -> false, received::add);

        assertThat(handler.importData(support(fileList(List.of(new File("a.jpg")))))).isFalse();
        assertThat(received).isEmpty();
    }

    private static TransferSupport support(Transferable data) {
        return new TransferSupport(new JPanel(), data);
    }

    private static Transferable fileList(List<File> files) {
        return new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] {DataFlavor.javaFileListFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return DataFlavor.javaFileListFlavor.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) {
                return files;
            }
        };
    }
}
