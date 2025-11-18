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

