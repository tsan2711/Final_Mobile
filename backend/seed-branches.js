require('dotenv').config();
const mongoose = require('mongoose');
const Branch = require('./src/models/Branch');

const seedBranches = async () => {
  try {
    // Connect to MongoDB
    await mongoose.connect(process.env.MONGODB_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true,
    });
    
    console.log('✅ MongoDB Connected for seeding branches');

    // Clear existing branches
    await Branch.deleteMany({});
    console.log('🗑️  Cleared existing branches');

    // Create branches
    const branches = [
      // 5 chi nhánh ban đầu
      {
        name: 'Chi nhánh Hồ Chí Minh - Trung tâm',
        address: '123 Nguyễn Huệ, Quận 1, TP.HCM',
        phone: '0283823456',
        latitude: 10.7769,
        longitude: 106.7009,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Hà Nội - Hoàn Kiếm',
        address: '456 Hoàng Kiếm, Quận Hoàn Kiếm, Hà Nội',
        phone: '0243823456',
        latitude: 21.0285,
        longitude: 105.8542,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Đà Nẵng',
        address: '789 Lê Duẩn, Quận Hải Châu, Đà Nẵng',
        phone: '0236382345',
        latitude: 16.0544,
        longitude: 108.2022,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Hồ Chí Minh - Quận 7',
        address: '321 Nguyễn Thị Thập, Quận 7, TP.HCM',
        phone: '0283823457',
        latitude: 10.7306,
        longitude: 106.7178,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Hà Nội - Cầu Giấy',
        address: '159 Trần Duy Hưng, Quận Cầu Giấy, Hà Nội',
        phone: '0243823457',
        latitude: 21.0301,
        longitude: 105.8019,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      // 20 chi nhánh mới
      {
        name: 'Chi nhánh Hồ Chí Minh - Quận 3',
        address: '45 Võ Văn Tần, Quận 3, TP.HCM',
        phone: '0283823458',
        latitude: 10.7870,
        longitude: 106.6920,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Hồ Chí Minh - Quận 10',
        address: '78 Nguyễn Tri Phương, Quận 10, TP.HCM',
        phone: '0283823459',
        latitude: 10.7730,
        longitude: 106.6670,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Hồ Chí Minh - Bình Thạnh',
        address: '156 Xô Viết Nghệ Tĩnh, Quận Bình Thạnh, TP.HCM',
        phone: '0283823460',
        latitude: 10.8100,
        longitude: 106.7100,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Hồ Chí Minh - Tân Bình',
        address: '234 Hoàng Văn Thụ, Quận Tân Bình, TP.HCM',
        phone: '0283823461',
        latitude: 10.8000,
        longitude: 106.6500,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Hà Nội - Ba Đình',
        address: '12 Nguyễn Trãi, Quận Ba Đình, Hà Nội',
        phone: '0243823458',
        latitude: 21.0350,
        longitude: 105.8400,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Hà Nội - Đống Đa',
        address: '89 Láng Hạ, Quận Đống Đa, Hà Nội',
        phone: '0243823459',
        latitude: 21.0100,
        longitude: 105.8200,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Hà Nội - Hai Bà Trưng',
        address: '234 Bạch Mai, Quận Hai Bà Trưng, Hà Nội',
        phone: '0243823460',
        latitude: 21.0150,
        longitude: 105.8500,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Hà Nội - Thanh Xuân',
        address: '567 Nguyễn Trãi, Quận Thanh Xuân, Hà Nội',
        phone: '0243823461',
        latitude: 20.9950,
        longitude: 105.8000,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Hải Phòng',
        address: '123 Lạch Tray, Quận Ngô Quyền, Hải Phòng',
        phone: '0225382345',
        latitude: 20.8560,
        longitude: 106.6820,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Cần Thơ',
        address: '456 Nguyễn Văn Cừ, Quận Ninh Kiều, Cần Thơ',
        phone: '0292382345',
        latitude: 10.0450,
        longitude: 105.7470,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Đà Nẵng - Sơn Trà',
        address: '78 Hoàng Diệu, Quận Sơn Trà, Đà Nẵng',
        phone: '0236382346',
        latitude: 16.0600,
        longitude: 108.2400,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Nha Trang',
        address: '234 Trần Phú, Nha Trang, Khánh Hòa',
        phone: '0258382345',
        latitude: 12.2380,
        longitude: 109.1960,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Huế',
        address: '123 Lê Lợi, Thành phố Huế, Thừa Thiên Huế',
        phone: '0234382345',
        latitude: 16.4630,
        longitude: 107.5950,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Vũng Tàu',
        address: '45 Trương Công Định, Vũng Tàu, Bà Rịa - Vũng Tàu',
        phone: '0254382345',
        latitude: 10.3460,
        longitude: 107.0840,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Quy Nhon',
        address: '89 Lê Lợi, Quy Nhon, Bình Định',
        phone: '0256382345',
        latitude: 13.7690,
        longitude: 109.2330,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Đà Lạt',
        address: '156 Trần Phú, Đà Lạt, Lâm Đồng',
        phone: '0263382345',
        latitude: 11.9400,
        longitude: 108.4380,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Buôn Ma Thuột',
        address: '234 Phan Chu Trinh, Buôn Ma Thuột, Đắk Lắk',
        phone: '0262382345',
        latitude: 12.6670,
        longitude: 108.0500,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Pleiku',
        address: '45 Lê Lợi, Pleiku, Gia Lai',
        phone: '0269382345',
        latitude: 13.9830,
        longitude: 108.0000,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Quảng Ninh',
        address: '78 Bạch Đằng, Hạ Long, Quảng Ninh',
        phone: '0203382345',
        latitude: 20.9100,
        longitude: 107.0800,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Thái Nguyên',
        address: '123 Hoàng Văn Thụ, Thái Nguyên',
        phone: '0208382345',
        latitude: 21.5940,
        longitude: 105.8480,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Nam Định',
        address: '234 Trần Hưng Đạo, Nam Định',
        phone: '0228382345',
        latitude: 20.4200,
        longitude: 106.1680,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      },
      {
        name: 'Chi nhánh Thanh Hóa',
        address: '156 Lê Lợi, Thanh Hóa',
        phone: '0237382345',
        latitude: 19.8070,
        longitude: 105.7760,
        openingHours: '8:00 - 17:00',
        services: ['Giao dịch', 'Tư vấn', 'ATM'],
        isActive: true
      }
    ];

    // Insert branches
    const createdBranches = await Branch.insertMany(branches);
    console.log(`✅ Created ${createdBranches.length} branches:`);
    createdBranches.forEach(branch => {
      console.log(`   - ${branch.name} (${branch.address})`);
    });

    console.log('\n🎉 Branch seeding completed successfully!');
    process.exit(0);
  } catch (error) {
    console.error('❌ Error seeding branches:', error);
    process.exit(1);
  }
};

// Run seed
seedBranches();

