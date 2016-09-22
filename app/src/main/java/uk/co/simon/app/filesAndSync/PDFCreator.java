package uk.co.simon.app.filesAndSync;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.CMYKColor;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.splunk.mint.Mint;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import uk.co.simon.app.R;
import uk.co.simon.app.sqllite.DataSource;
import uk.co.simon.app.sqllite.DataSourceLocations;
import uk.co.simon.app.sqllite.DataSourcePhotos;
import uk.co.simon.app.sqllite.DataSourceProjects;
import uk.co.simon.app.sqllite.DataSourceReportItems;
import uk.co.simon.app.sqllite.DataSourceReports;
import uk.co.simon.app.sqllite.SQLLocation;
import uk.co.simon.app.sqllite.SQLPhoto;
import uk.co.simon.app.sqllite.SQLProject;
import uk.co.simon.app.sqllite.SQLReport;
import uk.co.simon.app.sqllite.SQLReportItem;

public class PDFCreator extends AsyncTask<Void, Void, String> {

	SQLReport mThisReport;
	Context mContext;
	MaterialDialog mProgress;
	String mPath;
	DataSource mDataSource;

	public PDFCreator(SQLReport report, Context context, MaterialDialog progress) {
		mThisReport = report;
		mContext = context;
		mProgress = progress;
        mDataSource = new DataSource(mContext);
	}

	@Override
	protected String doInBackground(Void... params) {
		mPath = null;
		try {
			mPath = createPDF();
		} catch (DocumentException e) {
			return "Document Exception: " + e.toString();
		} catch (IOException e) {
			return "I/O Exception: " + e.toString();
		}
		return null;
	}

	protected void onPostExecute(String result) {
		String message;
		if (result==null){			
			mThisReport.setPDF(mPath);
			DataSourceReports datasource = new DataSourceReports(mDataSource);
			datasource.updateReport(mThisReport);
			message = mContext.getString(R.string.pdfSuccess);
		} else {
			message = mContext.getString(R.string.pdfFailure);
			Mint.logEvent(result);
		}
		Toast toast = Toast.makeText(mContext, message, Toast.LENGTH_LONG);
        mProgress.dismiss();
		toast.show();
        mDataSource.close();
	}

	public String createPDF() throws DocumentException, IOException {

		DataSourceProjects datasource = new DataSourceProjects(mDataSource);
		DataSourceReportItems datasourceReportItems = new DataSourceReportItems(mDataSource);
		DataSourcePhotos datasourcePhotos = new DataSourcePhotos(mDataSource);
		DataSourceLocations datasourceLocations = new DataSourceLocations(mDataSource);

        FileManager fm = new FileManager(mContext);

		float heightUsed = 0f;
		float leftRightMargins = 0f;
		float topMargin = 60f;
		float bottomMargin = 60f;

		//Set Fonts
		Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
		Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
		Font paragraphFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
		Font behindFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, CMYKColor.WHITE);

		Document doc = new Document();
		String pdfPath = getOutputFile(mThisReport, mContext);
		OutputStream pdfFile = new FileOutputStream(pdfPath);
		PdfWriter.getInstance(doc, pdfFile);
		doc.setMargins(leftRightMargins, leftRightMargins, topMargin, bottomMargin);
		doc.open();
		float pageHeight = doc.getPageSize().getHeight();
		float pageWidth = doc.getPageSize().getWidth();
		pageHeight = pageHeight - bottomMargin;

		PdfPTable title = new PdfPTable(2);
		float[] titleColumnWidths = {30f, 70f};
		title.setWidths(titleColumnWidths);
		title.setTotalWidth(pageWidth);
		title.setSpacingBefore(0);
		title.setSpacingAfter(15);

		PdfPCell titleImageCell = new PdfPCell();

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
		String customLogoPath = sharedPref.getString("imagePicker", null);
		boolean useDefaultLogo = true;

		if (customLogoPath != null ) {
			File customLogo = new File(customLogoPath);
			if (customLogo.exists()) {
				useDefaultLogo = false;
			}
		}

		if (useDefaultLogo) {
			Bitmap logoImageResource = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.logo_white_bg);
			ByteArrayOutputStream logoStream = new ByteArrayOutputStream();
			logoImageResource.compress(Bitmap.CompressFormat.JPEG, 80, logoStream);
			Image logo = Image.getInstance(logoStream.toByteArray());
			logo.setCompressionLevel(9);
			logo.scaleToFit(100, 220);
			titleImageCell.addElement(logo);
			titleImageCell.setHorizontalAlignment(Element.ALIGN_BOTTOM);
			titleImageCell.setBorder(Rectangle.NO_BORDER);
			titleImageCell.setPaddingTop(5);
			title.addCell(titleImageCell);
			logoImageResource.recycle();
		} else {
			final BitmapFactory.Options logoOptions = new BitmapFactory.Options();
			logoOptions.inJustDecodeBounds = false;
			Bitmap rawImage = BitmapFactory.decodeFile(customLogoPath, logoOptions);
			float scale = Math.min(416f/((float) rawImage.getWidth()), 1);
			Bitmap scaledBitmap = Bitmap.createScaledBitmap(rawImage, (int)Math.ceil(rawImage.getWidth()*scale), (int)Math.ceil(rawImage.getHeight()*scale), true);
			ByteArrayOutputStream logoStream = new ByteArrayOutputStream();
			scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, logoStream);
			Image logo = Image.getInstance(logoStream.toByteArray());
			logo.setCompressionLevel(9);
			logo.scaleToFit(100, 220);
			titleImageCell.addElement(logo);
			titleImageCell.setHorizontalAlignment(Element.ALIGN_BOTTOM);
			titleImageCell.setBorder(Rectangle.NO_BORDER);
			titleImageCell.setPaddingTop(5);
			title.addCell(titleImageCell);			
			rawImage.recycle();
			scaledBitmap.recycle();
		}

		PdfPCell titleCell = new PdfPCell();
		Paragraph titlePara = new Paragraph();
		if (mThisReport.getReportType()) {
			titlePara.add(mContext.getResources().getString(R.string.pdfSiteVisitReportTitle));
		} else {
			titlePara.add(mContext.getResources().getString(R.string.pdfProgressReportTitle));
		}
		titlePara.add(" - ");
		titlePara.add(mThisReport.getReportDate());
		titlePara.setSpacingAfter(5);
		titlePara.setFont(titleFont);
		titlePara.setAlignment(Element.ALIGN_RIGHT);
		titleCell.addElement(titlePara);

		SQLProject project = datasource.getProject(mThisReport.getProjectId());
		Paragraph projectPara = new Paragraph();
		projectPara.add(project.getProject());
		projectPara.setAlignment(Element.ALIGN_RIGHT);
		projectPara.setFont(paragraphFont);
		projectPara.setSpacingAfter(3);
		titleCell.addElement(projectPara);

		Paragraph authorPara = new Paragraph();
		authorPara.add(mThisReport.getSupervisor());
		authorPara.setAlignment(Element.ALIGN_RIGHT);
		authorPara.setFont(paragraphFont);
		titleCell.addElement(authorPara);

		titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		titleCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
		titleCell.setBorder(Rectangle.NO_BORDER);

		title.addCell(titleCell);
		doc.add(title);
		heightUsed = title.getTotalHeight();

		Paragraph reportRefPara = new Paragraph();
		reportRefPara.add(new Chunk(mContext.getResources().getString(R.string.dailyProgressReportRef) +
				": " + mThisReport.getReportRef(), subtitleFont));
		reportRefPara.setAlignment(Element.ALIGN_LEFT);
		reportRefPara.setSpacingAfter(10);
		reportRefPara.setFont(subtitleFont);
		reportRefPara.setIndentationLeft(60f);
		doc.add(reportRefPara);

		heightUsed = heightUsed + 20f;

		java.util.List<SQLReportItem> reportItems = datasourceReportItems.getReportItems(mThisReport.getId());

		int i = 1;

		for (SQLReportItem reportItem : reportItems) {

			PdfPTable reportItemTable = new PdfPTable(3);

			float[] columnWidths = {10f, 30f, 60f};
			reportItemTable.setWidths(columnWidths);
			reportItemTable.setTotalWidth(pageWidth);

			//Header Row
			PdfPCell headerRowCellLeft = new PdfPCell();
			PdfPCell headerRowCellRight = new PdfPCell();

			Paragraph itemNoPara = new Paragraph();
			Paragraph itemStatusPara = new Paragraph();

			String[] onTimeArray = mContext.getResources().getStringArray(R.array.dailyProgressOnTimeSpinner);
			if (reportItem.getOnTIme().contains(onTimeArray[0]) && !mThisReport.getReportType()) {
				headerRowCellLeft.setBackgroundColor(new CMYKColor(17,0,52,27));
				headerRowCellLeft.setBorderColor(new CMYKColor(17,0,52,27));
				headerRowCellRight.setBackgroundColor(new CMYKColor(17,0,52,27));
				headerRowCellRight.setBorderColor(new CMYKColor(17,0,52,27));
				itemNoPara.setFont(paragraphFont);
				itemStatusPara.setFont(paragraphFont);
			} else if (reportItem.getOnTIme().contains(onTimeArray[2]) && !mThisReport.getReportType()) {
				headerRowCellLeft.setBackgroundColor(new CMYKColor(0,100,100,39));
				headerRowCellLeft.setBorderColor(new CMYKColor(0,100,100,39));
				headerRowCellRight.setBackgroundColor(new CMYKColor(0,100,100,39));
				headerRowCellRight.setBorderColor(new CMYKColor(0,100,100,39));
				itemNoPara.setFont(behindFont);
				itemStatusPara.setFont(behindFont);
			} else {
				headerRowCellLeft.setBackgroundColor(new CMYKColor(0,20,100,0));
				headerRowCellLeft.setBorderColor(new CMYKColor(0,20,100,0));
				headerRowCellRight.setBackgroundColor(new CMYKColor(0,20,100,0));
				headerRowCellRight.setBorderColor(new CMYKColor(0,20,100,0));
				itemNoPara.setFont(paragraphFont);
				itemStatusPara.setFont(paragraphFont);
			}	

			itemNoPara.add(mContext.getResources().getString(R.string.pdfItem) + ": " + i);
			headerRowCellLeft.addElement(itemNoPara);
			headerRowCellLeft.setColspan(2);
			reportItemTable.addCell(headerRowCellLeft);

			if (!mThisReport.getReportType()) {
				itemStatusPara.add(mContext.getResources().getString(R.string.pdfProgress) +	": " +
						reportItem.getProgress() + "% - " + reportItem.getOnTIme());
				itemStatusPara.setAlignment(Element.ALIGN_RIGHT);
			}

			headerRowCellRight.addElement(itemStatusPara);
			reportItemTable.addCell(headerRowCellRight);

			//Row 2

			PdfPCell locationTitleCell = new PdfPCell();
			Paragraph locationTitle = new Paragraph();
			locationTitle.add(mContext.getResources().getString(R.string.dailyProgressLocation));
			locationTitle.setFont(paragraphFont);
			locationTitleCell.addElement(locationTitle);
			locationTitleCell.setBorderWidth(0);
			reportItemTable.addCell(locationTitleCell);

			PdfPCell locationCell = new PdfPCell();
			Paragraph locationPara = new Paragraph();
			if (reportItem.getLocationId()>0) {
				SQLLocation location = datasourceLocations.getLocation(reportItem.getLocationId());
				locationPara.add(location.getLocation());
				locationPara.setFont(paragraphFont);
			}
			locationCell.addElement(locationPara);
			locationCell.setBorderWidth(0);
			reportItemTable.addCell(locationCell);

			PdfPCell photosCell = new PdfPCell();

			java.util.List<SQLPhoto> photos = datasourcePhotos.getReportItemPhotos(reportItem.getId());
			if (photos.size()>1) {
				PdfPTable photoTable = new PdfPTable(2);
				float[] photoColumnWidths = {50f, 50f};
				photoTable.setWidths(photoColumnWidths);
				photoTable.setTotalWidth(pageWidth*0.7f);
				photoTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
				photoTable.getDefaultCell().setPadding(5f);

				int x = 0;

                for (SQLPhoto photo : photos) {
                    Bitmap scaledBitmap = fm.resizeImage(photo.getPhotoPath(), 520);
                    ByteArrayOutputStream photoStream = new ByteArrayOutputStream();
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, photoStream);
                    Image photoImage = Image.getInstance(photoStream.toByteArray());
                    photoImage.setCompressionLevel(9);
                    photoTable.addCell(photoImage);
                    x++;
                    scaledBitmap.recycle();
                }
				if (x % 2 != 0) {
					PdfPCell blankCell = new PdfPCell();
					blankCell.addElement(new Paragraph(" "));
					blankCell.setBorder(Rectangle.NO_BORDER);
					photoTable.addCell(blankCell);
				}
				photosCell.addElement(photoTable);

			} else if (photos.size() == 1) {

				PdfPTable photoTable = new PdfPTable(1);
				float[] photoColumnWidths = {100f};
				photoTable.setWidths(photoColumnWidths);
				photoTable.setTotalWidth(pageWidth*0.7f);
				photoTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
                photoTable.getDefaultCell().setPadding(5f);

                Bitmap scaledBitmap = fm.resizeImage(photos.get(0).getPhotoPath(), 1040);
                ByteArrayOutputStream photoStream = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, photoStream);
                Image photoImage = Image.getInstance(photoStream.toByteArray());
                photoImage.setCompressionLevel(9);
                photoTable.addCell(photoImage);
                scaledBitmap.recycle();
                photosCell.addElement(photoTable);

			} else {

				PdfPTable photoTable = new PdfPTable(1);
				float[] photoColumnWidths = {100f};
				photoTable.setWidths(photoColumnWidths);
				photoTable.setTotalWidth(pageWidth*0.7f);
				photoTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
				photoTable.getDefaultCell().setPadding(5f);
				Bitmap scaledBitmap = fm.resizeImage("Default", 1040);
				ByteArrayOutputStream photoStream = new ByteArrayOutputStream();
				scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, photoStream);
				Image photoImage = Image.getInstance(photoStream.toByteArray());
				photoImage.setCompressionLevel(9);
				photoTable.addCell(photoImage);
				scaledBitmap.recycle();
				photosCell.addElement(photoTable);
				
			}
			photosCell.setBorderWidth(0);
			photosCell.setRowspan(3);
			reportItemTable.addCell(photosCell);

			//Row 3

			PdfPCell activityTitleCell = new PdfPCell();
			Paragraph activityTitle = new Paragraph();
			activityTitle.add(mContext.getResources().getString(R.string.dailyProgressActivity));
			activityTitle.setFont(paragraphFont);
			activityTitleCell.addElement(activityTitle);
			activityTitleCell.setBorderWidth(0);
			reportItemTable.addCell(activityTitleCell);

			PdfPCell activityCell = new PdfPCell();
			Paragraph activityPara = new Paragraph();
			activityPara.add(reportItem.getReportItem());
			activityPara.setFont(paragraphFont);
			activityCell.addElement(activityPara);
			activityCell.setBorderWidth(0);
			reportItemTable.addCell(activityCell);

			//Row 4

			PdfPCell descriptionTitleCell = new PdfPCell();
			Paragraph descriptionTitle = new Paragraph();
			descriptionTitle.add(mContext.getResources().getString(R.string.dailyProgressDescription) +
					": " + reportItem.getDescription());
			descriptionTitle.setFont(paragraphFont);
			descriptionTitleCell.addElement(descriptionTitle);
			descriptionTitleCell.setBorderWidth(0);
			descriptionTitleCell.setColspan(2);
			reportItemTable.addCell(descriptionTitleCell);

			i++;
			reportItemTable.setSpacingAfter(25);

			if (heightUsed + reportItemTable.calculateHeights() > pageHeight) {
				doc.newPage();
				heightUsed = reportItemTable.getTotalHeight();
			} else {
				heightUsed = heightUsed + reportItemTable.getTotalHeight();
			}
			doc.add(reportItemTable);

		}
		doc.close();

		return pdfPath;
	}

	@SuppressLint("SimpleDateFormat")
	private static String getOutputFile(SQLReport thisReport, Context context) throws IOException {

		File StorageDir = FileManager.getPDFStorageLocation(context);

		if (StorageDir!=null) {
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd_").format(new Date());
			char fileSep = '/'; // ... or do this portably.
			char escape = '-'; // ... or some other legal char.
			int len = thisReport.getReportRef().length();
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < len; i++) {
				char ch = thisReport.getReportRef().charAt(i);
				if (ch < ' ' || ch >= 0x7F || ch == fileSep || (ch == '.') || ch == escape) {
					sb.append(escape);
				} else {
					sb.append(ch);
				}
			}
			return StorageDir.getPath() + File.separator + timeStamp + sb.toString() + ".pdf";
		} else {
			return null;
		}
	}
}
