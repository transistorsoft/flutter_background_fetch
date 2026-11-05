require 'yaml'
pubspec = YAML.load_file('../pubspec.yaml')
#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.cocoapods_version   = '>= 1.10.0'
  s.name             = pubspec['name']
  s.version          = pubspec['version']
  s.summary          = pubspec['description']
  s.description      = pubspec['description']
  s.homepage         = pubspec['homepage']

  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Transistor Software' => 'info@transistorsoft.com' }
  s.source           = { :path => '.' }
  s.source_files = 'background_fetch/Sources/background_fetch/**/*.{h,m}'
  s.public_header_files = 'background_fetch/Sources/background_fetch/include/**/*.h'
  s.dependency 'Flutter'
  # 4.2 is the first iOS 15 build; 4.1.x declares iOS 12, which Xcode 27 rejects (WO-001)
  s.dependency 'TSBackgroundFetch', '~> 4.2'
  s.ios.deployment_target = '15.0'
end

