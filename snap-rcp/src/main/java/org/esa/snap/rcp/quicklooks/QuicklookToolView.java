/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.rcp.quicklooks;

import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.quicklooks.Quicklook;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.window.OpenRGBImageViewAction;
import org.esa.snap.rcp.util.SelectionSupport;
import org.esa.snap.tango.TangoIcons;
import org.netbeans.api.annotations.common.NullAllowed;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

@TopComponent.Description(
        preferredID = "QuicklookToolView",
        iconBase = "org/esa/snap/rcp/icons/quicklook.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = "navigator",
        openAtStartup = false,
        position = 1
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.quicklooks.QuicklookToolView")
@ActionReferences({
        @ActionReference(path = "Menu/View/Tool Windows"),
        @ActionReference(path = "Toolbars/Analysis")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_QuicklookToolView_Name",
        preferredID = "QuicklookToolView"
)
@NbBundle.Messages({
        "CTL_QuicklookToolView_Name=Quicklooks",
        "CTL_QuicklookToolView_Description=Quicklooks of all bands",
})
/**
 * Tool window to display quicklooks
 */
public class QuicklookToolView extends TopComponent {

    private Product currentProduct;
    private final SortedSet<Product> productSet;
    private final JLabel nameLabel = new JLabel();
    private final ImagePanel imgPanel = new ImagePanel();
    private final BufferedImage noDataImage;
    private JScrollPane imgScrollPanel;
    private JButton nextBtn, prevBtn, startBtn, endBtn, xBtn, refreshBtn;
    private ButtonActionListener actionListener = new ButtonActionListener();
    private boolean updateQuicklooks = false;
    private ProductNode oldNode = null;

    private static final String DEFAULT_QUICKLOOK = "Default";

    private static final ImageIcon closeIcon = TangoIcons.actions_process_stop(TangoIcons.Res.R22);

    private static final ImageIcon firstIcon = TangoIcons.actions_go_first(TangoIcons.Res.R32);
    private static final ImageIcon lastIcon = TangoIcons.actions_go_last(TangoIcons.Res.R32);
    private static final ImageIcon nextIcon = TangoIcons.actions_go_next(TangoIcons.Res.R32);
    private static final ImageIcon previousIcon = TangoIcons.actions_go_previous(TangoIcons.Res.R32);
    private static final ImageIcon refreshIcon = TangoIcons.actions_view_refresh(TangoIcons.Res.R32);

    public QuicklookToolView() {
        setLayout(new BorderLayout());
        setDisplayName(Bundle.CTL_QuicklookToolView_Name());
        setToolTipText(Bundle.CTL_QuicklookToolView_Description());
        add(createPanel(), BorderLayout.CENTER);
        noDataImage = createNoDataImage();

        final SnapApp snapApp = SnapApp.getDefault();
        snapApp.getProductManager().addListener(new ProductManagerListener());

        productSet = new TreeSet<>(new Comparator<Product>() {
            public int compare(Product p1, Product p2) {
                int ref1 = p1.getRefNo();
                int ref2 = p2.getRefNo();
                return ref1 < ref2 ? -1 : ref1 == ref2 ? 0 : 1;
            }
        });

        addProducts();

        updateButtons();

        snapApp.getSelectionSupport(ProductNode.class).addHandler(new SelectionSupport.Handler<ProductNode>() {
            @Override
            public void selectionChange(@NullAllowed ProductNode oldValue, @NullAllowed ProductNode newValue) {
                if (newValue != null && newValue != oldNode) {
                    showProduct(newValue.getProduct());
                    oldNode = newValue;
                }
            }
        });
    }

    public JComponent createPanel() {

        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(createTopPanel(), BorderLayout.NORTH);
        panel.add(createImagePanel(), BorderLayout.CENTER);
        panel.add(createButtonPanel(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createTopPanel() {
        final JPanel topPanel = new JPanel(new BorderLayout());

        final JPanel productPanel = new JPanel(new BorderLayout());
        productPanel.add(nameLabel, BorderLayout.CENTER);

        final JPanel topButtonPanel = new JPanel(new BorderLayout());
        xBtn = createButton("Close", "Close Product", topButtonPanel, actionListener, closeIcon);
        topButtonPanel.add(xBtn, BorderLayout.EAST);

        topPanel.add(topButtonPanel, BorderLayout.CENTER);
        topPanel.add(productPanel, BorderLayout.SOUTH);

        return topPanel;
    }

    private JScrollPane createImagePanel() {
        imgScrollPanel = new JScrollPane(imgPanel);
        return imgScrollPanel;
    }

    private JPanel createButtonPanel() {
        final JPanel buttonPanel = new JPanel();

        startBtn = createButton("Start", "Go to first product", buttonPanel, actionListener, firstIcon);
        prevBtn = createButton("Prev", "Previous product", buttonPanel, actionListener, previousIcon);
        nextBtn = createButton("Next", "Next product", buttonPanel, actionListener, nextIcon);
        endBtn = createButton("End", "Go to last product", buttonPanel, actionListener, lastIcon);
        refreshBtn = createButton("Refresh", "Update products", buttonPanel, actionListener, refreshIcon);

        buttonPanel.add(startBtn);
        buttonPanel.add(prevBtn);
        buttonPanel.add(nextBtn);
        buttonPanel.add(endBtn);
        buttonPanel.add(refreshBtn);

        return buttonPanel;
    }

    private static JButton createButton(final String name, final String text, final JPanel panel,
                                        final ButtonActionListener actionListener, final ImageIcon icon) {
        final JButton btn = new JButton();
        btn.setName(name);
        btn.setIcon(icon);
        if (panel != null) {
            btn.setBackground(panel.getBackground());
        }
        btn.setToolTipText(text);
        btn.setActionCommand(name);
        btn.addActionListener(actionListener);

        return btn;
    }

    private void updateButtons() {
        if (productSet.isEmpty()) {
            startBtn.setEnabled(false);
            prevBtn.setEnabled(false);
            nextBtn.setEnabled(false);
            endBtn.setEnabled(false);
            xBtn.setEnabled(false);
            imgPanel.setImage(null);
            nameLabel.setText("");
            currentProduct = null;
        } else {
            startBtn.setEnabled(true);
            prevBtn.setEnabled(getPreviousProduct() != null);
            nextBtn.setEnabled(getNextProduct() != null);
            endBtn.setEnabled(true);
            xBtn.setEnabled(true);
        }
        refreshBtn.setEnabled(true);
    }

    private BufferedImage createNoDataImage() {
        final int w = 100, h = 100;
        final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        final Graphics g = image.createGraphics();

        g.setColor(Color.DARK_GRAY);
        //g.setStroke(new BasicStroke(1));
        g.drawLine(0, 0, 0 + w, h);
        g.drawLine(0 + w, 0, 0, h);
        g.drawRect(0, 0, w - 1, h - 1);

        return image;
    }

    private void addProducts() {

        final Product[] products = SnapApp.getDefault().getProductManager().getProducts();
        Collections.addAll(productSet, products);
    }

    private void loadProducts() {
        for (Product product : productSet) {
            if (product.getFileLocation() != null) {
                loadImage(product);
            }
        }
    }

    private synchronized void showProduct(final Product product) {
        if (product == null) {
            return;
        }

        if (updateQuicklooks) {
            if (product.getFileLocation() != null) {
                loadImage(product);
            }
        }
        setImage(product, DEFAULT_QUICKLOOK);
    }

    private void loadImage(final Product product) {
        ProgressMonitorSwingWorker<BufferedImage, Object> loader = new ProgressMonitorSwingWorker<BufferedImage, Object>
                (SnapApp.getDefault().getMainFrame(), "Loading quicklook image...") {

            @Override
            protected BufferedImage doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {
                return product.getDefaultQuicklook().getImage();
            }

            @Override
            protected void done() {
                showProduct(currentProduct);
            }
        };
        loader.execute();
    }

    private synchronized void setImage(final Product product, final String qlName) {

        currentProduct = product;
        nameLabel.setText(product.getDisplayName());

        Quicklook quicklook;
        if (qlName.equals(DEFAULT_QUICKLOOK)) {
            quicklook = currentProduct.getDefaultQuicklook();
        } else {
            quicklook = currentProduct.getQuicklook(qlName);
        }
        if (quicklook == null) {
            SystemUtils.LOG.severe("Quicklook " + qlName + " not found");
            return;
        }

        BufferedImage img;
        if(quicklook.hasImage() || quicklook.hasCachedQuicklook()) {
            img = quicklook.getImage();
        } else {
            img = noDataImage;
        }
        imgPanel.setImage(img);

        updateButtons();
    }

    private synchronized void removeImage(final Product product) {
        productSet.remove(product);
    }

    private Product getFirstProduct() {
        return productSet.isEmpty() ? null : productSet.first();
    }

    private Product getLastProduct() {
        return productSet.isEmpty() ? null : productSet.last();
    }

    private Product getNextProduct() {
        final Iterator<Product> itr = productSet.iterator();
        while (itr.hasNext()) {
            Product p = itr.next();
            if (p == currentProduct) {
                if (itr.hasNext()) {
                    return itr.next();
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    private Product getPreviousProduct() {
        final Iterator<Product> itr = productSet.iterator();
        Product prev = null;
        while (itr.hasNext()) {
            Product p = itr.next();
            if (p == currentProduct) {
                return prev;
            }
            prev = p;
        }
        return null;
    }

    private class ImagePanel extends JLabel implements MouseListener {

        private BufferedImage img = null;

        public ImagePanel() {
            setHorizontalAlignment(JLabel.CENTER);
            setVerticalAlignment(JLabel.CENTER);

            addMouseListener(this);
        }

        public void setImage(final BufferedImage img) {
            this.img = img;
            if (img == null) {
                setIcon(null);
            }
            //invalidate();
            repaint();
        }

        @Override
        public void paintComponent(Graphics g) {
            if (img != null) {
                final float ratio = img.getWidth() / (float) img.getHeight();
                final int w = imgScrollPanel.getWidth() - 10;
                final int h = imgScrollPanel.getHeight() - 10;
                int newW = w;
                int newH = h;
                if (w < h) {
                    newH = (int) (w * ratio);
                } else {
                    newW = (int) (h * ratio);
                }

                setIcon(new ImageIcon(img.getScaledInstance(newW, newH, BufferedImage.SCALE_FAST)));
            }
            super.paintComponent(g);
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            if (e.getClickCount() == 2 && currentProduct != null) {
                OpenRGBImageViewAction rgbAction = new OpenRGBImageViewAction(currentProduct);
                rgbAction.openProductSceneViewRGB(currentProduct, "");
            }
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }
    }

    private class ButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            switch (e.getActionCommand()) {
                case "Start":
                    showProduct(getFirstProduct());
                    break;
                case "Prev":
                    showProduct(getPreviousProduct());
                    break;
                case "Next":
                    showProduct(getNextProduct());
                    break;
                case "End":
                    showProduct(getLastProduct());
                    break;
                case "Refresh":
                    updateQuicklooks = true;
                    loadProducts();
                    break;
                case "Close":
                    if (currentProduct != null) {
                        SnapApp.getDefault().getProductManager().removeProduct(currentProduct);
                    }
                    break;
            }
        }
    }

    public class ProductManagerListener implements ProductManager.Listener {

        @Override
        public void productAdded(ProductManager.Event event) {
            productSet.add(event.getProduct());
            updateButtons();
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            if (event.getProduct() == currentProduct) {
                getNextProduct();
            }
            removeImage(event.getProduct());
            updateButtons();
        }
    }
}
