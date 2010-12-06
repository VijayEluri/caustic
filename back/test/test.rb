#!/usr/bin/ruby

###
#   SimpleScraper Back 0.0.1
#
#   Copyright 2010, AUTHORS.txt
#   Licensed under the MIT license.
#
#   test/test.rb : Tests.
###

$:<< Dir.pwd + '/..'

require 'SimpleScraper'
require 'test/unit'
require 'rack/test'

ENV['RACK_ENV'] = 'test'

module SimpleScraper
  class SimpleScraperTest < Test::Unit::TestCase
    include Rack::Test::Methods

    def app
      Sinatra::Application
    end

    def test_1_it_creates_type
      put '/type/Property'
      assert last_response.ok?, last_response.body
    end

    def test_2_it_adds_publish_fields
      put '/type/Property/publish/streetNum'
      assert last_response.ok?, last_response.body
    end
  end
end

# URL="http://localhost:4567"

# echo
# curl -X POST $URL/namespace/C36047
# curl -X POST $URL/namespace/NY.NYC.Brooklyn
# curl -X POST $URL/namespace/NY.NYC
# curl -X POST $URL/namespace/C36047/parent/NY.NYC.Brooklyn
# curl -X POST $URL/namespace/NY.NYC.Brooklyn/parent/NY.NYC
# curl -X POST $URL/type/Property
# curl -X POST $URL/type/Property/publish/streetNum
# curl -X POST $URL/type/Property/publish/streetDir
# curl -X POST $URL/type/Property/publish/streetName
# curl -X POST $URL/type/Property/publish/streetSuffix
# curl -X POST $URL/type/Property/publish/city
# curl -X POST $URL/type/Property/publish/zip
# curl -X POST $URL/type/Party
# curl -X POST $URL/type/Party/publish/name
# curl -X POST $URL/type/Party/publish/address
# curl -X POST $URL/information/NY.NYC.Brooklyn/Property
# curl -X POST -d "value=3" $URL/information/NY.NYC.Brooklyn/Property/default/boroughNumber

# curl -X POST $URL/gatherer/NY.NYC.BIS.Profile
# curl -X POST -d "value=http://a810-bisweb.nyc.gov/bisweb/PropertyProfileOverviewServlet" $URL/gatherer/NY.NYC.BIS.Profile/url
# curl -X POST $URL/gatherer/NY.NYC.DOF.PropertyAddressSearch
# curl -X POST -d "value=http://webapps.nyc.gov:8084/CICS/FIN1/FIND001I" $URL/gatherer/NY.NYC.DOF.PropertyAddressSearch/url
# curl -X POST $URL/gatherer/NY.NYC.ACRIS.Parcel.All
# curl -X POST -d "value=http://a836-acris.nyc.gov/Scripts/DocSearch.dll/BBLResult"  $URL/gatherer/NY.NYC.ACRIS.Parcel.All/url
# curl -X POST $URL/gatherer/NY.NYC.ACRIS.Parcel.Deeds
# curl -X POST $URL/gatherer/NY.NYC.ACRIS.Parcel.Deeds/parent/NY.NYC.ACRIS.Parcel.All
# curl -X POST $URL/gatherer/NY.NYC.ACRIS.Parcel.Mortgages
# curl -X POST $URL/gatherer/NY.NYC.ACRIS.Parcel.Mortgages/parent/NY.NYC.ACRIS.Parcel.All
# curl -X POST -d "value=$R{boroughNumber}" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/post/hid_borough
# curl -X POST -d "value=$R{block}" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/post/hid_block
# curl -X POST -d "value=$R{lot}" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/post/hid_lot
# curl -X POST -d "value=http://a836-acris.nyc.gov/Scripts/DocSearch.dll/BBL" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/header/Referer
# curl -X POST -d "value=50" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/get/max_rows
# curl -X POST -d "value=To Current Date" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/post/hid_selectdate
# curl -X POST -d "value=" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/post/hid_datefromm
# curl -X POST -d "value=" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/post/hid_datefromd
# curl -X POST -d "value=" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/post/hid_datefromy
# curl -X POST -d "value=" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/post/hid_datetom
# curl -X POST -d "value=" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/post/hid_datetod
# curl -X POST -d "value=" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/post/hid_datetoy
# curl -X POST -d "value=" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/post/hid_doctype_name
# curl -X POST -d "value=50" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/post/hid_max_rows
# curl -X POST -d "value=" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/post/hid_page
# curl -X POST -d "value=" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/post/hid_ReqID
# curl -X POST -d "value=N" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/post/hid_ISIntranet
# curl -X POST -d "value=" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/post/hid_EmployeeID
# curl -X POST -d "value=BBL" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/post/hid_SearchType
# curl -X POST -d "value=YES" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/cookie/JUMPPAGE
# curl -X POST -d "value=MORTGAGES & INSTRUMENTS" $URL/gatherer/NY.NYC.ACRIS.Parcel.Mortgages/post/hid_doctype_name
# curl -X POST -d "value=ALL_MORT" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/post/hid_doctype
# curl -X POST -d "value=DEEDS AND OTHER CONVEYANCES" $URL/gatherer/NY.NYC.ACRIS.Parcel.Mortgages/post/hid_doctype_name
# curl -X POST -d "value=ALL_DEED" $URL/gatherer/NY.NYC.ACRIS.Parcel.All/post/hid_doctype
# curl -X POST -d "value=http://a810-bisweb.nyc.gov/bisweb/bispi00.jsp" $URL/gatherer/NY.NYC.BIS.Profile/header/Referer
# curl -X POST -d "value= GO " $URL/gatherer/NY.NYC.BIS.Profile/get/go2
# curl -X POST -d "value=0" $URL/gatherer/NY.NYC.BIS.Profile/get/requestid
# curl -X POST -d "value=$R{boroughNumber}" $URL/gatherer/NY.NYC.BIS.Profile/get/boro
# curl -X POST -d "value=$R{streetNumber}" $URL/gatherer/NY.NYC.BIS.Profile/get/houseno
# curl -X POST -d "value=$R{streetDir} $R{streetName} $R{streetSuffix}" $URL/gatherer/NY.NYC.BIS.Profile/get/street
# curl -X POST -d "value=http://webapps.nyc.gov:8084/CICS/fin1/find001I" $URL/gatherer/NY.NYC.DOF.PropertyAddressSearch/header/Referer
# curl -X POST -d "value=" $URL/gatherer/NY.NYC.DOF.PropertyAddressSearch/post/FAPTNUM
# curl -X POST -d "value=A" $URL/gatherer/NY.NYC.DOF.PropertyAddressSearch/post/FFUNC
# curl -X POST -d "value=SEARCH" $URL/gatherer/NY.NYC.DOF.PropertyAddressSearch/post/DFH_ENTER
# curl -X POST -d "value=$R{boroughNumber}" $URL/gatherer/NY.NYC.DOF.PropertyAddressSearch/post/FBORO
# curl -X POST -d "value=$R{streetNumber}" $URL/gatherer/NY.NYC.DOF.PropertyAddressSearch/post/FHOUSENUM
# curl -X POST -d "value=$R{streetDir} $R{streetName} $R{streetSuffix}" $URL/gatherer/NY.NYC.DOF.PropertyAddressSearch/post/FSTNAME

# curl -X POST $URL/information/NY.NYC/Property/gatherer/NY.NYC.BIS.Profile
# curl -X POST $URL/information/NY.NYC/Property/gatherer/NY.NYC.DOF.PropertyAddressSearch
# curl -X POST $URL/information/NY.NYC/Property/gatherer/NY.NYC.ACRIS.Parcel.Deeds
# curl -X POST $URL/information/NY.NYC/Property/gatherer/NY.NYC.ACRIS.Parcel.Mortgages

# curl -X POST -d "regex=<!--Table Begin!-->([\s\S]+?)<!--Table End-->" $URL/information/NY.NYC/Property/gatherer.NY.NYC.ACRIS.Parcel.Deeds/0/to/ACRIS.Parcel.Deeds.table
# curl -X POST -d "regex=<!--Table Begin!-->([\s\S]+?)<!--Table End-->" $URL/information/NY.NYC/Property/gatherer.NY.NYC.ACRIS.Parcel.Mortgages/0/to/ACRIS.Parcel.Mortgages.table
# curl -X POST -d "regex=BIN#[^\d*](\d+)" $URL/information/NY.NYC/Property/gatherer.NY.NYC.BIS.Profile/0/to/BIN
# curl -X POST -d "regex=BIN#[^\d*](\d+)" $URL/information/NY.NYC/Property/gatherer.NY.NYC.BIS.Profile/0/to/BIN
# curl -X POST -d "regex=<td class=\"maininfo\"[^>]*>[^<]*</td>[^<]*<td class=\"maininfo\"[^>]*>([^0-9]+)" $URL/information/NY.NYC/Property/gatherer.NY.NYC.BIS.Profile/0/to/BIS.city
# curl -X POST -d "<input\s+type=\"hidden\"\s+name=\"q49_block_id\"\s+value=\"([^\"]+)\">" $URL/information/NY.NYC/Property/gatherer.NY.NYC.DOF.PropertyAddressSearch/0/to/block
# curl -X POST -d "<input\s+type=\"hidden\"\s+name=\"q49_lot\"\s+value=\"([^\"]+)\">" $URL/information/NY.NYC/Property/gatherer.NY.NYC.DOF.PropertyAddressSearch/0/to/lot
