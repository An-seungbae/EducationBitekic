package advanced_excel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import java.text.SimpleDateFormat;
import advanced_excel.proxies.CellType;
import advanced_excel.proxies.CellFormat;
import advanced_excel.proxies.DocumentType;
import advanced_excel.proxies.TextAlignment;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;


public class Utils {
	static HashMap<String, Workbook> iWorkBooks = new HashMap<String, Workbook>();
	static HashMap<String, DocumentType> iDocumentTypes = new HashMap<String, DocumentType>();
	/*
	static Workbook iWorkBook;
	static DocumentType iDocumentType;
	
	public static void SetDocumentType(DocumentType doctype)
	{ iDocumentType = doctype; }
	
	public static void SetWorkBook(Workbook workbook)
	{ iWorkBook = workbook; }

	public static DocumentType GetDocumentType()
	{ return iDocumentType; }
	
	public static Workbook GetWorkBook()
	{ return iWorkBook; }
	*/
	
	public static String AddWorkBook(Workbook workbook, DocumentType doctype)
	{
		String uid = UUID.randomUUID().toString();
		iWorkBooks.put(uid, workbook);
		iDocumentTypes.put(uid, doctype);
		return uid;
	}
	
	public static void CloseWorkBook(String UID)
	{
		iWorkBooks.remove(UID);
		iDocumentTypes.remove(UID);
	}
	
	public static DocumentType GetDocumentType(String UID)
	{ return iDocumentTypes.get(UID); }
	
	public static Workbook GetWorkBook(String UID)
	{ return iWorkBooks.get(UID); }

	public static void SetCellValue(Workbook workbook, Cell cell, CellType cellType, String cellValue)
	{
		if (cellValue == null) { return; }
		if (cellValue.equals(null) || cellValue.equals("")) { return; }
		try
		{
			if (cellType == CellType.decimal)
			{
				cell.setCellType(org.apache.poi.ss.usermodel.CellType.NUMERIC);
				cell.setCellValue(Double.parseDouble(cellValue));
			}
			else if (cellType == CellType.integer)
			{
				cell.setCellType(org.apache.poi.ss.usermodel.CellType.NUMERIC);
				cell.setCellValue(Integer.parseInt(cellValue));
			}
			else if (cellType == CellType._boolean)
			{
				cell.setCellType(org.apache.poi.ss.usermodel.CellType.BOOLEAN);
				cell.setCellValue(Boolean.parseBoolean(cellValue));
			}
			else if (cellType == CellType.datetime)
			{
				if (!cellValue.equals(null) && !cellValue.equals(""))
				{ cell.setCellValue(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(cellValue)); }
			}
			else if (cellType == CellType.formula)
			{
				cell.setCellFormula(cellValue);
			}
			else
			{
				cell.setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
				cell.setCellValue(cellValue);
			}
		} catch (Exception e) {
			logger.error("ERROR in Advanced_Excel.SetCellValue: " + e.getMessage() + "\n" + e.toString(), e);
		}
	}
	
	public static void CreateStyle(Workbook workbook, CellStyle style, CellFormat cellFormat, DocumentType docType)
	{
		try
		{
			if (cellFormat != null)
			{
				HSSFPalette palette = null;
				if (docType == DocumentType.XLS) { palette = ((HSSFWorkbook)workbook).getCustomPalette(); }
				
				advanced_excel.proxies.Color BkgColor = cellFormat.getBackgroundColor();
				if (BkgColor != null) {
					if (docType == DocumentType.XLS) { ((HSSFCellStyle)style).setFillForegroundColor(palette.findSimilarColor(BkgColor.getr(), BkgColor.getg(), BkgColor.getb()).getIndex()); }
					else { ((XSSFCellStyle)style).setFillForegroundColor(new XSSFColor(new java.awt.Color(BkgColor.getr(), BkgColor.getg(), BkgColor.getb()), new DefaultIndexedColorMap())); }
					style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
				}
				
				TextAlignment align = cellFormat.getTextAlignment();
				if (align == TextAlignment.Center) {
					style.setAlignment(HorizontalAlignment.CENTER);
				} else if (align == TextAlignment.Right) {
					style.setAlignment(HorizontalAlignment.RIGHT);
				} else {
					style.setAlignment(HorizontalAlignment.LEFT);
				}
				
				advanced_excel.proxies.BorderStyle borderStyle = cellFormat.getBorderBottom();
				BorderStyle border = getBorderStyle(borderStyle);
				style.setBorderBottom(border);
				advanced_excel.proxies.Color borderColor = cellFormat.getBorderBottom_Color();
				if (borderColor != null) {
					if (docType == DocumentType.XLS) { ((HSSFCellStyle)style).setBottomBorderColor(palette.findSimilarColor(borderColor.getr(), borderColor.getg(), borderColor.getb()).getIndex()); }
					else { ((XSSFCellStyle)style).setBottomBorderColor(new XSSFColor(new java.awt.Color(borderColor.getr(), borderColor.getg(), borderColor.getb()), new DefaultIndexedColorMap())); }
				}
				
				borderStyle = cellFormat.getBorderTop();
				border = getBorderStyle(borderStyle);
				style.setBorderTop(border);
				borderColor = cellFormat.getBorderTop_Color();
				if (borderColor != null) {
					if (docType == DocumentType.XLS) { ((HSSFCellStyle)style).setTopBorderColor(palette.findSimilarColor(borderColor.getr(), borderColor.getg(), borderColor.getb()).getIndex()); }
					else { ((XSSFCellStyle)style).setTopBorderColor(new XSSFColor(new java.awt.Color(borderColor.getr(), borderColor.getg(), borderColor.getb()), new DefaultIndexedColorMap())); }
				}
				
				borderStyle = cellFormat.getBorderLeft();
				border = getBorderStyle(borderStyle);
				style.setBorderLeft(border);
				borderColor = cellFormat.getBorderLeft_Color();
				if (borderColor != null) {
					if (docType == DocumentType.XLS) { ((HSSFCellStyle)style).setLeftBorderColor(palette.findSimilarColor(borderColor.getr(), borderColor.getg(), borderColor.getb()).getIndex()); }
					else { ((XSSFCellStyle)style).setLeftBorderColor(new XSSFColor(new java.awt.Color(borderColor.getr(), borderColor.getg(), borderColor.getb()), new DefaultIndexedColorMap())); }
				}
				
				borderStyle = cellFormat.getBorderRight();
				border = getBorderStyle(borderStyle);
				style.setBorderRight(border);
				borderColor = cellFormat.getBorderRight_Color();
				if (borderColor != null) {
					if (docType == DocumentType.XLS) { ((HSSFCellStyle)style).setRightBorderColor(palette.findSimilarColor(borderColor.getr(), borderColor.getg(), borderColor.getb()).getIndex()); }
					else { ((XSSFCellStyle)style).setRightBorderColor(new XSSFColor(new java.awt.Color(borderColor.getr(), borderColor.getg(), borderColor.getb()), new DefaultIndexedColorMap())); }
				}
				
				if (docType == DocumentType.XLS)
				{
					HSSFFont font = ((HSSFWorkbook)workbook).createFont();
					String FontName = cellFormat.getFontName();
					if (FontName != null) {
						font.setFontName(FontName);
					}
					Integer FontSize = cellFormat.getFontSize();
					if (FontSize != null) {
						font.setFontHeightInPoints(FontSize.shortValue());
					}
					advanced_excel.proxies.Color FontColor = cellFormat.getFontColor();
					if (FontColor != null) {
						font.setColor(palette.findSimilarColor(FontColor.getr(), FontColor.getg(), FontColor.getb()).getIndex()); 
					}
					font.setBold(cellFormat.getBold());
					((HSSFCellStyle)style).setFont(font);
				}
				else
				{
					XSSFFont font = ((XSSFWorkbook)workbook).createFont();
					String FontName = cellFormat.getFontName();
					if (FontName != null) {
						font.setFontName(FontName);
					}
					Integer FontSize = cellFormat.getFontSize();
					if (FontSize != null) {
						font.setFontHeightInPoints(FontSize.shortValue());
					}
					advanced_excel.proxies.Color FontColor = cellFormat.getFontColor();
					if (FontColor != null) {
						font.setColor(new XSSFColor(new java.awt.Color(FontColor.getr(), FontColor.getg(), FontColor.getb()), new DefaultIndexedColorMap()));
					}
					font.setBold(cellFormat.getBold());
					((XSSFCellStyle)style).setFont(font);
				}
				
				String CustomFormat = cellFormat.getCustomFormat();
				if (CustomFormat != null) {
					style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(CustomFormat));
				}
			}
		} catch (Exception e) {
			logger.error("ERROR in Advanced_Excel.SetCellValue: " + e.getMessage() + "\n" + e.toString(), e);
		}
	}
	
	public static BorderStyle getBorderStyle(advanced_excel.proxies.BorderStyle borderStyle)
	{
		BorderStyle value = BorderStyle.NONE;
		switch (borderStyle) {
			case NONE:
				value = BorderStyle.NONE;
				break;
			case THICK:
				value = BorderStyle.THICK;
				break;
			case THIN:
				value = BorderStyle.THIN;
				break;
			case DASH_DOT:
				value = BorderStyle.DASH_DOT;
				break;
			case DASH_DOT_DOT:
				value = BorderStyle.DASH_DOT_DOT;
				break;
			case DASHED:
				value = BorderStyle.DASHED;
				break;
			case DOTTED:
				value = BorderStyle.DOTTED;
				break;
			case DOUBLE_LINE:
				value = BorderStyle.DOUBLE;
				break;
			case HAIR_LINE:
				value = BorderStyle.HAIR;
				break;
			case MEDIUM:
				value = BorderStyle.MEDIUM;
				break;
			case MEDIUM_DASH_DOT:
				value = BorderStyle.MEDIUM_DASH_DOT;
				break;
			case MEDIUM_DASH_DOT_DOT:
				value = BorderStyle.MEDIUM_DASH_DOT_DOT;
				break;
			case MEDIUM_DASHED:
				value = BorderStyle.MEDIUM_DASHED;
				break;
			case SLANTED_DASH_DOT:
				value = BorderStyle.SLANTED_DASH_DOT;
				break;
			default:
				value = BorderStyle.NONE;
				break;
		}
		return value;
	}
	
	protected static ILogNode logger = Core.getLogger("Advanced_Excel");
}
