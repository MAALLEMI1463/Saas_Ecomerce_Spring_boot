package com.example.demo.Service;

import com.example.demo. Model.Order;
import com.example.demo.Model.OrderItem;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class InvoiceService {

    // ─── Main colors ──────────────────────────────────────────
    private static final Color COLOR_PRIMARY  = new Color(34, 34, 34);
    private static final Color COLOR_ACCENT   = new Color(59, 130, 246);
    private static final Color COLOR_LIGHT    = new Color(245, 245, 245);
    private static final Color COLOR_WHITE    = Color.WHITE;
    private static final Color COLOR_BORDER   = new Color(220, 220, 220);

    // ─── Fonts ────────────────────────────────────────────────
    private static final Font FONT_TITLE =
        new Font(Font.HELVETICA, 22, Font.BOLD, COLOR_PRIMARY);
    private static final Font FONT_SUBTITLE =
        new Font(Font.HELVETICA, 10, Font.NORMAL,
                 new Color(120, 120, 120));
    private static final Font FONT_SECTION =
        new Font(Font.HELVETICA, 11, Font.BOLD, COLOR_PRIMARY);
    private static final Font FONT_NORMAL =
        new Font(Font.HELVETICA, 10, Font.NORMAL, COLOR_PRIMARY);
    private static final Font FONT_SMALL =
        new Font(Font.HELVETICA, 9, Font.NORMAL,
                 new Color(100, 100, 100));
    private static final Font FONT_TABLE_HEADER =
        new Font(Font.HELVETICA, 10, Font.BOLD, COLOR_WHITE);
    private static final Font FONT_TABLE_CELL =
        new Font(Font.HELVETICA, 10, Font.NORMAL, COLOR_PRIMARY);
    private static final Font FONT_TOTAL =
        new Font(Font.HELVETICA, 12, Font.BOLD, COLOR_PRIMARY);
    private static final Font FONT_ACCENT =
        new Font(Font.HELVETICA, 10, Font.BOLD, COLOR_ACCENT);

    // ─── Generate PDF bytes ───────────────────────────────────
    public byte[] generateInvoice(Order order) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);

            doc.open();

            // ── 1. Header ──────────────────────────────────────
            addHeader(doc, order);

            // ── 2. Divider ─────────────────────────────────────
            addDivider(doc);

            // ── 3. Client Info + Invoice Meta ──────────────────
            addClientAndMeta(doc, order);

            doc.add(Chunk.NEWLINE);

            // ── 4. Items Table ─────────────────────────────────
            addItemsTable(doc, order);

            doc.add(Chunk.NEWLINE);

            // ── 5. Total ───────────────────────────────────────
            addTotal(doc, order);

            doc.add(Chunk.NEWLINE);
            doc.add(Chunk.NEWLINE);

            // ── 6. Footer ──────────────────────────────────────
            addFooter(doc, order);

            doc.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to generate invoice: " + e.getMessage(), e);
        }
    }

    // ─── HEADER ───────────────────────────────────────────────
    private void addHeader(Document doc, Order order)
            throws DocumentException {

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1.5f, 1f});

        // Left: Store name + tagline
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPaddingBottom(10);

        Paragraph storeName = new Paragraph(
            order.getUser().getSubdomain().toUpperCase(),
            FONT_TITLE);
        leftCell.addElement(storeName);

        Paragraph tagline = new Paragraph(
            "Sales Invoice / فاتورة بيع", FONT_SUBTITLE);
        leftCell.addElement(tagline);

        // Right: Invoice number badge
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.setPaddingBottom(10);

        Paragraph invoiceNum = new Paragraph(
            "INVOICE #" + String.format("%05d", order.getId()),
            new Font(Font.HELVETICA, 14, Font.BOLD, COLOR_ACCENT));
        invoiceNum.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(invoiceNum);

        Paragraph status = new Paragraph(
            "● Cash on Delivery (COD)",
            new Font(Font.HELVETICA, 9, Font.NORMAL,
                     new Color(34, 197, 94)));
        status.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(status);

        header.addCell(leftCell);
        header.addCell(rightCell);
        doc.add(header);
    }

    // ─── DIVIDER ──────────────────────────────────────────────
    private void addDivider(Document doc)
            throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COLOR_ACCENT);
        cell.setFixedHeight(3f);
        cell.setBorder(Rectangle.NO_BORDER);
        line.addCell(cell);
        doc.add(line);
        doc.add(Chunk.NEWLINE);
    }

    // ─── CLIENT INFO + INVOICE META ───────────────────────────
    private void addClientAndMeta(Document doc, Order order)
            throws DocumentException {

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 1f});

        // ── Left: Client Info ──────────────────────────────
        PdfPCell clientCell = new PdfPCell();
        clientCell.setBorder(Rectangle.NO_BORDER);
        clientCell.setBackgroundColor(COLOR_LIGHT);
        clientCell.setPadding(12);

        clientCell.addElement(
            new Paragraph("CLIENT INFORMATION", FONT_SECTION));
        clientCell.addElement(Chunk.NEWLINE);

        clientCell.addElement(labelValue(
            "📞 Phone",    order.getPhone()));
        clientCell.addElement(labelValue(
            "🗺 Wilaya",   order.getWilaya()));
        clientCell.addElement(labelValue(
            "🏘 Commune",  order.getCommune()));
        clientCell.addElement(labelValue(
            "💳 Payment",  "Cash on Delivery"));

        // ── Right: Invoice Meta ────────────────────────────
        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorder(Rectangle.NO_BORDER);
        metaCell.setPadding(12);

        metaCell.addElement(
            new Paragraph("INVOICE DETAILS", FONT_SECTION));
        metaCell.addElement(Chunk.NEWLINE);

        String dateStr = order.getCreatedAt() != null
            ? order.getCreatedAt().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            : "N/A";

        metaCell.addElement(labelValue(
            "Invoice No.",
            "#" + String.format("%05d", order.getId())));
        metaCell.addElement(labelValue("Date",      dateStr));
        metaCell.addElement(labelValue(
            "Status",     order.getStatus()));
        metaCell.addElement(labelValue(
            "Store",
            order.getUser().getSubdomain()));

        table.addCell(clientCell);
        table.addCell(metaCell);
        doc.add(table);
    }

    // ─── ITEMS TABLE ──────────────────────────────────────────
    private void addItemsTable(Document doc, Order order)
            throws DocumentException {

        // Section title
        Paragraph title = new Paragraph(
            "ORDER ITEMS", FONT_SECTION);
        title.setSpacingAfter(6);
        doc.add(title);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 1f, 1.5f, 1.5f});

        // ── Table headers ──────────────────────────────────
        String[] headers = {
            "Product", "Qty", "Unit Price", "Subtotal"
        };
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(
                new Phrase(h, FONT_TABLE_HEADER));
            cell.setBackgroundColor(COLOR_PRIMARY);
            cell.setPadding(8);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setHorizontalAlignment(
                h.equals("Product")
                    ? Element.ALIGN_LEFT
                    : Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // ── Table rows ─────────────────────────────────────
        boolean alternate = false;
        for (OrderItem item : order.getOrderItems()) {

            Color rowBg = alternate
                ? COLOR_LIGHT : COLOR_WHITE;
            alternate = !alternate;

            // Product name
            table.addCell(styledCell(
                item.getProduct().getName(),
                FONT_TABLE_CELL, rowBg,
                Element.ALIGN_LEFT));

            // Quantity
            table.addCell(styledCell(
                String.valueOf(item.getQuantity()),
                FONT_TABLE_CELL, rowBg,
                Element.ALIGN_CENTER));

            // Unit price
            table.addCell(styledCell(
                String.format("%.2f DA",
                    item.getPrice().doubleValue()),
                FONT_TABLE_CELL, rowBg,
                Element.ALIGN_CENTER));

            // Subtotal
            double subtotal = item.getPrice().doubleValue()
                * item.getQuantity();
            table.addCell(styledCell(
                String.format("%.2f DA", subtotal),
                new Font(Font.HELVETICA, 10,
                         Font.BOLD, COLOR_PRIMARY),
                rowBg,
                Element.ALIGN_CENTER));
        }

        doc.add(table);
    }

    // ─── TOTAL ────────────────────────────────────────────────
    private void addTotal(Document doc, Order order)
            throws DocumentException {

        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(45);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.setWidths(new float[]{1f, 1f});

        // Total row
        PdfPCell labelCell = new PdfPCell(
            new Phrase("TOTAL AMOUNT", FONT_TOTAL));
        labelCell.setBackgroundColor(COLOR_ACCENT);
        labelCell.setPadding(10);
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell valueCell = new PdfPCell(
            new Phrase(
                String.format("%.2f DA",
                    order.getTotalPrice().doubleValue()),
                new Font(Font.HELVETICA, 12,
                         Font.BOLD, COLOR_WHITE)));
        valueCell.setBackgroundColor(COLOR_ACCENT);
        valueCell.setPadding(10);
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        totalTable.addCell(labelCell);
        totalTable.addCell(valueCell);
        doc.add(totalTable);
    }

    // ─── FOOTER ───────────────────────────────────────────────
    private void addFooter(Document doc, Order order)
            throws DocumentException {

        addDivider(doc);

        Paragraph footer = new Paragraph(
            "Thank you for your order! — "
            + order.getUser().getSubdomain().toUpperCase()
            + " Store",
            FONT_SMALL);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);

        Paragraph note = new Paragraph(
            "This invoice is generated automatically. "
            + "For any questions, please contact the store.",
            FONT_SMALL);
        note.setAlignment(Element.ALIGN_CENTER);
        doc.add(note);
    }

    // ─── HELPERS ──────────────────────────────────────────────
    private Paragraph labelValue(String label, String value) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", FONT_SMALL));
        p.add(new Chunk(value != null ? value : "—",
                        FONT_NORMAL));
        p.setSpacingAfter(3);
        return p;
    }

    private PdfPCell styledCell(String text, Font font,
                                Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(7);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBorderWidthBottom(0.5f);
        cell.setBorderColorBottom(COLOR_BORDER);
        cell.setHorizontalAlignment(align);
        return cell;
    }
}