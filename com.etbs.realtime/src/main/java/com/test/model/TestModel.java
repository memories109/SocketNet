package com.realtime.model;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.realtime.App;
import com.realtime.Constants;
import com.realtime.annotation.BindValue;
import com.realtime.enums.AUTO_APPL_HDL_CD;
import com.realtime.service.HS10Service;
import com.realtime.util.DateUtil;
import com.realtime.util.StringUtil;

public class HS10Model {
	
	private static final Logger log = LoggerFactory.getLogger(HS10Model.class);
	
	private HS10Service service;
	
	public HS10Model() {
		
	}
	
	public HS10Model(String telegramSource) throws UnsupportedEncodingException, IllegalArgumentException, IllegalAccessException {		
		
		byte[] bytes = telegramSource.getBytes(Constants.ENCODE_TYPE);
		
		Field[] fields = this.getClass().getDeclaredFields();
		
		for(Field field : fields) {
			
			field.setAccessible(true);						
			
			BindValue bindValue = field.getAnnotation(BindValue.class);
			
			if(bindValue != null) {
		
				int startPosition = bindValue.StartPosition();
				int length = bindValue.Length();
				String name = bindValue.Name();
				
				if(length > 0) {							
					if(field.getType().getCanonicalName().equalsIgnoreCase("int")) {
						field.set(this, StringUtil.trimWhitespaceToInt(bytes, startPosition,length));
					}
					else {
						field.set(this, StringUtil.trimWhitespace(bytes, startPosition,length));
					}
				}
				
				String value = field.get(this).toString();
				
				System.out.println(String.format("%s =  %s | %s(%s)", name, value, value.length(), length));
			}
		}
		
		// 서비스 할당
		service = App.context.getBean(HS10Service.class);
					
		// 분할, 할부가 아닌경우 고객사사업자번호+CI로 사용자 정보를 업데이트 함
		if(EnableDvied() == false && IsallPayType() == true) {
			
			memberInfo = service.getMemberInfo(ci, designCode);
		}
		
		// 수신매출을 저장		
		service.setData(this);
		
	}
	
	private ResultModel result = new ResultModel();

	// 비용처리
	public ResultModel process() {
		
		// 분할인지 검사
		if(this.EnableDvied() == true) {		
			result.setCode(AUTO_APPL_HDL_CD.R10);
			result.setMessage(String.format("[%s불가] %s %s원", AUTO_APPL_HDL_CD.R10.getName() , this.storeName, this.getUseMoneyString()));
			result.setSuccess(false);
		}
		else if (this.IsallPayType() == false) {
			result.setCode(AUTO_APPL_HDL_CD.R20);
			result.setMessage(String.format("[%s불가] %s %s원", AUTO_APPL_HDL_CD.R20.getName() , this.storeName, this.getUseMoneyString()));
			result.setSuccess(false);
		}
		else if(this.memberInfo == null) {
			result.setCode(AUTO_APPL_HDL_CD.R90);
			result.setMessage(String.format("[%s불가] %s", AUTO_APPL_HDL_CD.R90.getName(), this.ci));
			result.setSuccess(false);
		}
		else if(this.memberInfo != null) {
			
			// 가족카드불가인지보고 처리
			if(this.memberInfo.EnableFamilyCard() == false && this.isFamilyUse() == true) {
				// 가족 카드 불가인데 가족카드 매출
				result.setCode(AUTO_APPL_HDL_CD.R30);
				result.setMessage(String.format("[%s불가] %s %s원", AUTO_APPL_HDL_CD.R30.getName() , this.storeName, this.getUseMoneyString()));
				result.setSuccess(false);
			}
			else {
				
				// 비용처리 시작
				// 이 과정을 통해 SP에서 returnCode, returnMessage가 할당 됨
				service.setApplData(this);
				
				AUTO_APPL_HDL_CD hdlCD = AUTO_APPL_HDL_CD.valueOf(returnCode);
				
				result.setCode(hdlCD);
				
				if(hdlCD == AUTO_APPL_HDL_CD.S03 || hdlCD == AUTO_APPL_HDL_CD.S04) {
					
					if("10".equals(this.getStatementTypeCode())) {
						// 정상
						result.setMessage("[포인트차감] "+this.getStoreName()+" "+this.getApplPntString()+"P");						
					}
					else {
						// 취소
						result.setMessage("[포인트차감취소] "+this.getStoreName()+" "+this.getApplPntString()+"P");
					}
					
					result.setSuccess(true); // 자동신청, 취소의 경우 성공
				}
				else {
					
					// 차감이 되지 않았다.
					// 미설정 항목
					if(hdlCD == AUTO_APPL_HDL_CD.XX) {
						result.setMessage(String.format("[대상아님] %s %s원 | %s >> XX",this.getStoreName(), this.getUseMoneyString(), this.memberInfo.getMbrNo()));
					}
					else if(hdlCD == AUTO_APPL_HDL_CD.F01) {
						result.setMessage(String.format("[불가] %s %s원 | %s >> F01", this.getStoreName(), this.getUseMoneyString(), this.memberInfo.getMbrNo()));
					}
					else if(hdlCD == AUTO_APPL_HDL_CD.P02) {
						result.setMessage(String.format("[포인트부족] %s %s원 | %s >> P02 | %s", this.getStoreName(), this.getUseMoneyString(), this.memberInfo.getMbrNo(), returnMessage));
					}
					else if(hdlCD == AUTO_APPL_HDL_CD.F02) {
						result.setMessage(String.format("[기신청] %s %s원 | %s >> %s", this.getStoreName(), this.getUseMoneyString(), this.memberInfo.getMbrNo(), this.realTimePK));
					}
					else if(hdlCD == AUTO_APPL_HDL_CD.P10) {
						result.setMessage(String.format("[신청항목없음] %s %s원 | %s >> %s", this.getStoreName(), this.getUseMoneyString(), this.memberInfo.getMbrNo(), this.realTimePK));
					}					
					else {
						result.setMessage(String.format("[오류] %s %s원 >> %s | %s",this.getStoreName(), this.getUseMoneyString(), returnCode, returnMessage));
					}
					
					result.setSuccess(false);
				}
				
				log.debug(result.getMessage());
			}
		}
		else {
			
			log.error("사용자 정보를 찾을 수 없음 : "+ci);
			
			result.setCode(AUTO_APPL_HDL_CD.R99);
			result.setMessage(String.format("[%s] %s %sP", AUTO_APPL_HDL_CD.R99.getName() , this.storeName, this.getUseMoneyString()));
			result.setSuccess(false);
		}
		
		result.setHs10Model(this);
		
		return result;
	}
	
	private MemberInfoModel memberInfo = null;
	
	private int seq;
	
	public int getSeq() {
		return seq;
	}

	public void setSeq(int seq) {
		this.seq = seq;
	}

	// 전문에서 자동으로 파싱됨
	@BindValue(StartPosition=0, 	Length=1	, Name="매출구분(1:승인/2:취소)")
	private String statementType; 	// 매출구분 > 1:승인, 2:취소
	
	@BindValue(StartPosition=1, 	Length=2	, Name="카드구분(01:신용/11:체크)")
	private String cardType; 		// 카드구분 > 01:신용, 11:체크
	
	@BindValue(StartPosition=3, 	Length=100	, Name="CI")
	private String ci;				// CI
	
	@BindValue(StartPosition=103, 	Length=4	, Name="카드번호4자리")
	private String card4No;		// 카드번호뒤네자리
	
	@BindValue(StartPosition=107, 	Length=1	, Name="일시불구분(1:일시불/2:할부)")
	private String allPayType;		// 일시불구분 > 1:일시불, 2:할부
	
	// 일시불 여부
	public boolean IsallPayType() {
		return "1".equals(allPayType);
	}
	
	public String getAllPayTypeCode() {
		// 할부카드유형코드 [WC020]
		if("1".equals(this.allPayType)) {
			return "10"; // 일시불
		}
		else if("2".equals(this.allPayType)) {
			return "20"; // 할부
		}
		else {
			return "30"; // 기타
		}
	}
	
	@BindValue(StartPosition=108, 	Length=10	, Name="승인번호")
	private String agreeNo;		// 승인번호
	
	@BindValue(StartPosition=118, 	Length=11	, Name="매출금액(사용금액)")
	private int useMoney;		// 매출금액
	
	@BindValue(StartPosition=129, 	Length=50	, Name="가맹점명")
	private String storeName;		// 가맹점명
	
	@BindValue(StartPosition=179, 	Length=1	, Name="가족구분(1:본인/2:가족)")
	private String familyType;		// 가족여부 1:본인, 2:가족
	
	@BindValue(StartPosition=180, 	Length=5	, Name="가맹점업종코드")
	private String storeTypeNo;// 가맹점업종코드
	
	@BindValue(StartPosition=185, 	Length=30	, Name="가맹점업종명")
	private String storeTypeName;	// 가맹점업종명
	
	@BindValue(StartPosition=215, 	Length=10	, Name="가맹점사업자번호")
	private String storeBizNo;	// 가맹점사업자번호
	
	@BindValue(StartPosition=225, 	Length=14	, Name="승인일자(사용일자)")
	private String useDate;			// 사용(승인)일자 14자리 YYYYMMDDHHmmss
	
	@BindValue(StartPosition=239, 	Length=5	, Name="상품코드")
	private String designCode;		// 카드상품 코드
	
	@BindValue(StartPosition=244, Length=60, Name="실시간PK")
	private String realTimePK;		// 실시간전문PK (HS13의 매칭 키가 됨)
	
	@BindValue(StartPosition=304, 	Length=1	, Name="분할매입가능여부(0:NO/1:YES)")
	private String enableDivied;	// 분할매입가능여부 0:NO, 1:YES
	
	// 분할매출가능여부
	public boolean EnableDvied() {
		return "1".equals(enableDivied);
	}
	
	/* 하나카드의 경우 아래 업종코드만 분할가능으로 들어옴
	6306	항공사
	6311	대한항공
	6309	아시아나항공
	6303	철도
	6101	호텔(특급)
	5205	농협식품전문점
	6102	호텔(특급외)	
	 */
	
	@BindValue(StartPosition=305, 	Length=20	, Name="가맹점번호")
	private String storeNo;	// 가맹점번호 11자리임	
	
	public MemberInfoModel getMemberInfo() {
		
		return memberInfo;
	}	

	public String getStatementType() {
		return statementType;
	}
	
	public String getStatementTypeCode() {
		
		// 카드매출유형코드 [WC019]
		if("1".equals(this.statementType)) {
			return "10"; // 정상
		}
		else if("2".equals(this.statementType)) {
			return "20"; // 취소
		}
		else {
			return "30"; // 없음
		}
	}

	public void setStatementType(String statementType) {
		this.statementType = statementType;
	}

	public String getCardType() {
		return cardType;
	}

	public void setCardType(String cardType) {
		this.cardType = cardType;
	}

	public String getCi() {
		return ci;
	}

	public void setCi(String ci) {
		this.ci = ci;
	}

	public String getCard4No() {
		return card4No;
	}

	public void setCard4No(String card4No) {
		this.card4No = card4No;
	}

	public String getAllPayType() {
		return allPayType;
	}

	public void setAllPayType(String allPayType) {
		this.allPayType = allPayType;
	}

	public String getAgreeNo() {
		return agreeNo;
	}

	public void setAgreeNo(String agreeNo) {
		this.agreeNo = agreeNo;
	}

	public int getUseMoney() {
		return useMoney;
	}

	public void setUseMoney(int reqMoney) {
		this.useMoney = reqMoney;
	}
	
	public String getUseMoneyString() {
		 return new DecimalFormat("#,###").format(useMoney);
	}

	public String getStoreName() {
		return storeName;
	}

	public void setStoreName(String storeName) {
		this.storeName = storeName;
	}

	public String getFamilyType() {
		return familyType;
	}
	
	public String getFamilyTypeCode() {
		// 가족카드유형코드 [WC021]
		if("1".equals(this.allPayType)) {
			return "10"; // 본인
		}
		else if("2".equals(this.allPayType)) {
			return "20"; // 가족
		}
		else {
			return "30"; // 기타
		}
	}
	
	public boolean isFamilyUse() {
		return "2".equalsIgnoreCase(familyType);
	}

	public void setFamilyType(String familyType) {
		this.familyType = familyType;
	}

	public String getStoreTypeNo() {
		return storeTypeNo;
	}

	public void setStoreTypeNo(String storeTypeNo) {
		this.storeTypeNo = storeTypeNo;
	}

	public String getStoreTypeName() {
		return storeTypeName;
	}

	public void setStoreTypeName(String storeTypeName) {
		this.storeTypeName = storeTypeName;
	}

	public String getStoreBizNo() {
		return storeBizNo;
	}

	public void setStoreBizNo(String storeBizNo) {
		this.storeBizNo = storeBizNo;
	}

	public String getUseDate() {
		return useDate;
	}

	public void setUseDate(String reqDate) {
		this.useDate = reqDate;
	}

	public String getDesignCode() {
		return designCode;
	}

	public void setDesignCode(String designCode) {
		this.designCode = designCode;
	}

	public String getEnableDivied() {
		return enableDivied;
	}

	public void setEnableDivied(String enableDivied) {
		this.enableDivied = enableDivied;
	}

	public String getStoreNo() {
		return storeNo;
	}

	public void setStoreNo(String storeNo) {
		this.storeNo = storeNo;
	}
	
	public Date getUseDateTime() {
		return DateUtil.getDateTime(this.useDate, "yyyyMMddHHmmss");
	}
	public String getUseDateTimeString() {
		return DateUtil.getDateString(getUseDateTime(), "yyyyMMdd");
	}
	
	public String getUseDateTimeString(String format) {
		return DateUtil.getDateString(getUseDateTime(), format);
	}
	
	// 비용처리 SP에서 담을 값
	
	private double applPnt = 0; // 차감금액

	public double getApplPnt() {
		return applPnt;
	}

	public void setApplPnt(double applPnt) {
		this.applPnt = applPnt;
	}
	
	private double remainPnt = 0; // 남은포인트
	

	public double getRemainPnt() {
		return remainPnt;
	}

	public void setRemainPnt(double remainPnt) {
		this.remainPnt = remainPnt;
	}
	
	public String getRemainPntString() {
		return new DecimalFormat("#,###.##").format(remainPnt);
	}
	
	// 차감 후 이전 포인트
	public double getBeforePnt() {
		return remainPnt+applPnt;
	}
	
	public String getBeforePntString() {
		return new DecimalFormat("#,###.##").format(getBeforePnt());
	}

	public String getApplPntString() {
		return new DecimalFormat("#,###.##").format(applPnt);
	}
	
	private String returnCode = "99";
	private String returnMessage = "";

	public String getReturnCode() {
		return returnCode;
	}

	public void setReturnCode(String returnCode) {
		this.returnCode = returnCode;
	}

	public String getReturnMessage() {
		return returnMessage;
	}

	public void setReturnMessage(String returnMessage) {
		this.returnMessage = returnMessage;
	}
}
